import java.net.URL
import org.kohsuke.github.GHEvent
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

@GrabResolver(name = 'central', root='https://repo1.maven.org/maven2/')
@Grapes([
    @Grab('org.kohsuke:github-api:1.313')
])

final GitHub gh = new GitHubBuilder()
        .withJwtToken(System.getenv('AUTH_TOKEN'))
        .build()
final URL hook = new URL(System.getenv('HOOK_URL'))
final List<GHEvent> events = List.of(
        GHEvent.PUSH,
        GHEvent.PULL_REQUEST,
        GHEvent.PULL_REQUEST_REVIEW,
        GHEvent.PULL_REQUEST_REVIEW_COMMENT,
        GHEvent.ISSUES,
        GHEvent.ISSUE_COMMENT,
        GHEvent.COMMIT_COMMENT,
        GHEvent.DISCUSSION,
        GHEvent.DISCUSSION_COMMENT,
        GHEvent.CREATE
);
gh.getRepository(System.getenv('REPO_NAME'))
    .createHook('web', ['url': hook.toExternalForm(), 'content_type': 'json'], events, true)