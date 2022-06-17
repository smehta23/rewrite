package org.openrewrite.java.dataflow2

import org.junit.jupiter.api.Test
import org.openrewrite.java.dataflow2.examples.ZipSlip
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class ZipSlipTest: RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(ZipSlip())
    }

    @Test
    fun fixesZipSlipUsingFile()  = rewriteRun(
        java(
            """
            import java.io.File;
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                FileOutputStream os = new FileOutputStream(file); // ZipSlip
                RandomAccessFile raf = new RandomAccessFile(file, "rw"); // ZipSlip
                FileWriter fw = new FileWriter(file); // ZipSlip
              }
            }
            """,
            """
            import java.io.File;
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.io.UncheckedIOException;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                if (!file.toPath().startsWith(dir.toPath())) {
                    throw new UncheckedIOException("ZipSlip attack detected");
                }
                FileOutputStream os = new FileOutputStream(file); // ZipSlip
                RandomAccessFile raf = new RandomAccessFile(file, "rw"); // ZipSlip
                FileWriter fw = new FileWriter(file); // ZipSlip
              }
            }
            """
        )
    )

    @Test
    fun fixesZipSlipUsingPath()  = rewriteRun(
        java(
            """
            import java.io.File;
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.nio.file.Files;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, Path dir) throws Exception {
                String name = entry.getName();
                Path file = dir.resolve(name);
                FileOutputStream os = Files.newOutputStream(file); // ZipSlip
              }
            }
            """,
            """
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.io.UncheckedIOException;
            import java.nio.file.Files;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, Path dir) throws Exception {
                String name = entry.getName();
                Path file = dir.resolve(name);
                if (file.startsWith(dir)) {
                    throw new UncheckedIOException("ZipSlip attack detected");
                }
                FileOutputStream os = Files.newOutputStream(file); // ZipSlip
              }
            }
            """
        )
    )

    @Test
    fun fixesZipSlipUsingString()  = rewriteRun(
        java(
            """
            import java.io.File;
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.nio.file.Files;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                FileOutputStream os = new FileOutputStream(dir + File.separator + name); // ZipSlip
              }
            }
            """,
            """
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.io.UncheckedIOException;
            import java.nio.file.Files;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                if (!file.toPath().startsWith(dir.toPath())) {
                    throw new UncheckedIOException("ZipSlip attack detected");
                }
                FileOutputStream os = new FileOutputStream(file.toString()); // ZipSlip
              }
            }
            """
        )
    )

    @Test
    fun safeZipSlipPathStartsWith() = rewriteRun(
        java(
            """
            import java.io.FileOutputStream;
            import java.io.File;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m2(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                File canFile = file.getCanonicalFile();
                String canDir = dir.getCanonicalPath();
                if (!canFile.toPath().startsWith(canDir))
                  throw new Exception();
                FileOutputStream os = new FileOutputStream(file); // OK
              }
            }
            """
        )
    )

    @Test
    fun safeZipSlipPathNormalizedStartsWith() = rewriteRun(
        java(
            """
            public class ZipTest {
              public void m3(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                if (!file.toPath().normalize().startsWith(dir.toPath()))
                  throw new Exception();
                FileOutputStream os = new FileOutputStream(file); // OK
              }
            }
            """
        )
    )

    @Test
    fun safeZipSlipValidateMethod() = rewriteRun(
        java(
            """
            public class ZipTest {
              private void validate(File tgtdir, File file) throws Exception {
                File canFile = file.getCanonicalFile();
                if (!canFile.toPath().startsWith(tgtdir.toPath()))
                  throw new Exception();
              }
              public void m4(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                validate(dir, file);
                FileOutputStream os = new FileOutputStream(file); // OK
              }
          }
          """
        )
    )

    @Test
    fun safeZipSlipPathAbsoluteNormalizeStartsWith() = rewriteRun(
        java(
            """
            public class ZipTest {
              public void m5(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                Path absfile = file.toPath().toAbsolutePath().normalize();
                Path absdir = dir.toPath().toAbsolutePath().normalize();
                if (!absfile.startsWith(absdir))
                  throw new Exception();
                FileOutputStream os = new FileOutputStream(file); // OK
              }
              }
            """
        )
    )

    @Test
    fun safeZipSlipSlipCanonicalPath() = rewriteRun(
        java(
            """
            public class ZipTest {
              public void m6(ZipEntry entry, Path dir) throws Exception {
                String canonicalDest = dir.toFile().getCanonicalPath();
                Path target = dir.resolve(entry.getName());
                String canonicalTarget = target.toFile().getCanonicalPath();
                if (!canonicalTarget.startsWith(canonicalDest + File.separator))
                  throw new Exception();
                OutputStream os = Files.newOutputStream(target); // OK
              }
            }
            """
        )
    )
}
