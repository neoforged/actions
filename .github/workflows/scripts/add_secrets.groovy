import java.net.URL
import org.kohsuke.github.*
import groovy.json.JsonSlurper

@GrabResolver(name = 'central', root='https://repo1.maven.org/maven2/')
@Grapes([
    @Grab('org.apache.groovy:groovy-json:4.0.13'),
    @Grab('org.kohsuke:github-api:1.313')
])

final inputs = new JsonSlurper().parse(new java.io.File(System.getenv('EVENT_PATH'))).inputs
final GitHub gh = new GitHubBuilder()
        .withJwtToken(System.getenv('AUTH_TOKEN'))
        .build()

public static OrgRepos getOrgSecret(GitHub gitHub, String org, String secret) throws IOException {
        final var sec = gitHub.createRequest()
                .withUrlPath("/orgs/" + org + "/actions/secrets/" + secret + "/repositories")
                .fetch(OrgRepos.class);
        sec.org = org;
        sec.name = secret;
        return sec;
}

public static class OrgRepos extends GitHubInteractiveObject {
        public List<GHRepository> repositories;

        private String org, name;

        public void addRepo(GHRepository repository) throws IOException {
                root().createRequest()
                        .withUrlPath("/orgs/" + org + "/actions/secrets/" + name + "/repositories")
                        .method("PUT")
                        .inBody()
                        .with("selected_repository_ids", Stream.concat(repositories.stream(), Stream.of(repository))
                                .map(repo -> repo.getId())
                                .distinct()
                                .toList())
                        .send();
        }

        public void removeRepo(GHRepository repository) throws IOException {
                root().createRequest()
                        .withUrlPath("/orgs/" + org + "/actions/secrets/" + name + "/repositories")
                        .method("PUT")
                        .inBody()
                        .with("selected_repository_ids", repositories.stream()
                                .filter(repo -> repo.getId() != repository.getId())
                                .map(repo -> repo.getId())
                                .toList())
                        .send();
        }
}

private static void addToSecret(GitHub gitHub, GHRepository target, String... secrets) throws IOException {
        for (String secret : secrets) {
            GitHubAccessor.getOrSecret(gitHub, target.getOwnerName(), secret).addRepo(target);
        }
}

private static void removeSecrets(GitHub gitHub, GHRepository target, String... secrets) throws IOException {
        for (String secret : secrets) {
            GitHubAccessor.getOrSecret(gitHub, target.getOwnerName(), secret).removeRepo(target);
        }
}

private static void modifySecrets(boolean add, GitHub gh, GHRepository target, String... secrets) throws IOException {
        if (add) addToSecret(gh, target, secrets)
        else removeSecrets(gh, target, secrets)
}

final repo = gh.getRepository(System.getenv('REPO_NAME'))

modifySecrets(inputs.maven_secrets, gh, repo, "MAVEN_USER", "MAVEN_PASSWORD")
modifySecrets(inputs.sonatype_secrets, gh, repo, "SONATYPE_USER", "SONATYPE_PASSWORD")
modifySecrets(inputs.gpp_secrets, gh, repo, "GPP_KEY", "GPP_SECRET")
modifySecrets(inputs.gpg_private_key, gh, repo, "GPG_PRIVATE_KEY", "GPG_KEY_PASSWORD")