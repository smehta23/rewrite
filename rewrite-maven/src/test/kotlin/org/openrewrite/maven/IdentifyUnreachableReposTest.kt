package org.openrewrite.maven

import org.junit.jupiter.api.Test

class IdentifyUnreachableReposTest : MavenRecipeTest {

    // HTTPS yields "ambiguous"
    @Test
    fun `https maven glassfish dead repo` () = assertChanged(
        recipe = IdentifyUnreachableRepos(),
        before = """
                <project>
                    <modelVersion>4.0.0</modelVersion>

                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>https://maven.glassfish.org/content/groups/public</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """,
        after = """
                <project>
                    <modelVersion>4.0.0</modelVersion>

                    <modelVersion>4.0.0</modelVersion>

                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    
                    <repositories>
                        <repository>
                            <id>central</id>
                            <!--~~(replacement)~~>--><url>https://maven.glassfish.org/content/groups/public</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """
    )

    // HTTP yields "dead"
    @Test
    fun `http maven glassfish dead repo within property` () = assertChanged(
        recipe = IdentifyUnreachableRepos(),
        before = """
                <project>
                    <properties>
                        <my-repo-url>http://maven.glassfish.org/content/groups/public</my-repo-url>
                    </properties>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>${'$'}{my-repo-url}</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """,
        after = """
                <project>
                    <properties>
                        <!--~~(replacement)~~>--><my-repo-url>http://maven.glassfish.org/content/groups/public</my-repo-url>
                    </properties>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>${'$'}{my-repo-url}</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """
    )

    // HTTP yields "dead"
    @Test
    fun `http maven glassfish dead repo` () = assertChanged(
        recipe = IdentifyUnreachableRepos(),
        before = """
                <project>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>http://maven.glassfish.org/content/groups/public</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """,
        after = """
                <project>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <!--~~(replacement)~~>--><url>http://maven.glassfish.org/content/groups/public</url>
                        </repository>
                        <repository>
                            <id>spring-snapshots</id>
                            <url>https://repo.spring.io/snapshot</url>
                        </repository>
                        <repository>
                            <id>spring-milestones</id>
                            <url>https://repo.spring.io/milestone</url>
                        </repository>
                    </repositories>
                </project>
            """
    )

    @Test
    fun removeNoDead() = assertUnchanged(
        recipe = IdentifyUnreachableRepos(),
        before = """
                <project>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>https://repo.maven.apache.org/maven2</url>
                        </repository>
                    </repositories>
                    <properties>
                        <spring-repo-snapshot>https://repo.spring.io/snapshot</spring-repo-snapshot>
                        <spring-repo-milestone>https://repo.spring.io/milestone</spring-repo-milestone>
                    </properties>
                </project>
            """
    )
}