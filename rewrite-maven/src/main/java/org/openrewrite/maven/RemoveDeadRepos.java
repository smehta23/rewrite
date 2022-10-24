package org.openrewrite.maven;

import lombok.AllArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.Recipe;

public class RemoveDeadRepos extends Recipe{
    private static final XPathMatcher REPOSITORY_URL_MATCHER = new XPathMatcher("/project/repositories/repository");
    private static final XPathMatcher PLUGIN_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/pluginRepositories/pluginRepository");
    private static final XPathMatcher DISTRIBUTION_MANAGEMENT_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/distributionManagement/repository");
    private static final XPathMatcher DISTRIBUTION_MANAGEMENT_SNAPSHOT_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/distributionManagement/snapshotRepository");

    @Override
    public String getDisplayName() {
        return "Remove dead repositories";
    }

    @Override
    public String getDescription() {
        return "Remove repos marked as dead in Maven POM files.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveDeadReposVisitor();
    }


    private static class RemoveDeadReposVisitor extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag newTag = super.visitTag(tag, ctx);
            if (isRepositoryTag()) {
                newTag = tag.getChild("url").flatMap(Xml.Tag::getValue).map(url -> {
                    // if the url is a property, check if the property's URL is in KNOWN_DEFUNCT; if so,
                    // replace/remove the property (if the property is removed, remove the repo as well)
                    if (url.startsWith("$")) {
                        String propertyName = url.substring(2, url.length() - 1);
                        doAfterVisit(new CheckPropertyLink(propertyName));
                        return tag;
                    }
                    // otherwise, simply check if the repo URL is in KNOWN_DEFUNCT; accordingly replace the
                    // repo's URL or delete this particular repo tag entirely
                    else {
                        return maybeDeleteOrReplaceUrlAndOrRepo(tag, url, false);
                    }
                }).orElse(null);
            }
            return newTag;
        }

        private boolean isRepositoryTag() {
            return REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    PLUGIN_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_SNAPSHOT_REPOSITORY_URL_MATCHER.matches(getCursor());
        }

    }

    @AllArgsConstructor
    private static class CheckPropertyLink extends MavenIsoVisitor<ExecutionContext> {
        final String propertyName;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag newPropTag = super.visitTag(tag, ctx);
            if (isPropertyTag() && propertyName.equals(tag.getName())) {
                newPropTag = maybeDeleteOrReplaceUrlAndOrRepo(tag, tag.getValue().orElse(""), true);
                if (newPropTag == null) {
                    doAfterVisit(new DeleteReposWithProperty(propertyName));
                }
            }
            return newPropTag;
        }
    }

    @AllArgsConstructor
    private static class DeleteReposWithProperty extends MavenIsoVisitor<ExecutionContext> {
        @NonNull
        final String propertyName;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            // if the url tag within a repository tag uses property propertyName, delete the repository tag
            if (isRepositoryTag() &&
                    propertyName.equals(
                            tag.getChildValue("url")
                                    .map(u -> u.substring(2, u.length() - 1))
                                    .orElse(null))) {
                return null;
            }
            return super.visitTag(tag, ctx);
        }

        private boolean isRepositoryTag() {
            return REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    PLUGIN_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_SNAPSHOT_REPOSITORY_URL_MATCHER.matches(getCursor());
        }
    }


    private static Xml.Tag maybeDeleteOrReplaceUrlAndOrRepo(Xml.Tag tag, String url, boolean replacingPropertyOnly) {
        String urlKey = IdentifyUnreachableRepos.httpOrHttps(url);
        if (IdentifyUnreachableRepos.KNOWN_ACTIVE.contains(urlKey)) {
            return tag;
        }
        if (IdentifyUnreachableRepos.KNOWN_DEFUNCT.containsKey(urlKey)) {
            if (IdentifyUnreachableRepos.KNOWN_DEFUNCT.get(urlKey) == null) {
                return null;
            }
            // we assume here that https:// is a valid prefix for the replacement URL
            if (replacingPropertyOnly) {
                return tag.withValue("https://" + IdentifyUnreachableRepos.KNOWN_DEFUNCT.get(urlKey));
            }
            return tag.withChildValue("url", "https://" + IdentifyUnreachableRepos.KNOWN_DEFUNCT.get(urlKey));
        }
        return tag;
    }
}