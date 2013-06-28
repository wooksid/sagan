package org.springframework.site.blog.feed;

import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;
import org.junit.Before;
import org.junit.Test;
import org.springframework.site.blog.Post;
import org.springframework.site.blog.PostBuilder;
import org.springframework.site.services.SiteUrl;
import org.springframework.ui.ExtendedModelMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class BlogPostAtomViewerTests {

	private ExtendedModelMap model = new ExtendedModelMap();
	private SiteUrl siteUrl;
	private BlogPostAtomViewer blogPostAtomViewer;
	private Feed feed = new Feed();
	private Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	private HttpServletRequest request = mock(HttpServletRequest.class);

	@Before
	public void setUp() throws Exception {
		siteUrl = mock(SiteUrl.class);
		blogPostAtomViewer = new BlogPostAtomViewer(siteUrl);
		when(request.getServerName()).thenReturn("springsource.org");
		model.addAttribute("posts", new ArrayList<Post>());
	}

	@Test
	public void hasFeedTitleFromModel() {
		model.addAttribute("feed-title", "Spring Engineering");
		blogPostAtomViewer.buildFeedMetadata(model, feed, mock(HttpServletRequest.class));
		assertThat(feed.getTitle(), is("Spring Engineering"));
	}

	@Test
	public void hasCategoryInLink() {
		String expectedFeedPath = "/blog/category/engineering.atom";
		String expectedFeedUrl = "http://localhost:8080/blog/category/engineering.atom";
		when(siteUrl.getAbsoluteUrl(eq(expectedFeedPath))).thenReturn(expectedFeedUrl);
		model.addAttribute("feed-path", expectedFeedPath);

		blogPostAtomViewer.buildFeedMetadata(model, feed, mock(HttpServletRequest.class));

		Link feedLink = (Link) feed.getAlternateLinks().get(0);
		assertThat(feedLink.getHref(), is(expectedFeedUrl));
	}

	@Test
	public void hasCorrectIdForFeed() throws Exception {
		model.addAttribute("feed-path", "/blog");

		blogPostAtomViewer.buildFeedMetadata(model, feed, request);

		assertThat(feed.getId(), is("tag:springsource.org:/blog"));
	}

	@Test
	public void feedUpdatedDateIsMostRecentPublishedPostDate() throws Exception {
		List<Post> posts = new ArrayList<Post>();
		buildPostsWithDate(5, posts);
		model.addAttribute("posts", posts);

		blogPostAtomViewer.buildFeedMetadata(model, feed, request);

		Post latestPost = posts.get(0);
		assertThat(feed.getUpdated(), is(latestPost.getPublishedDate()));
	}

	@Test
	public void feedUpdatedDateIsNotPresentWhenThereAreNoPosts() throws Exception {
		List<Post> noPosts = new ArrayList<Post>();
		model.addAttribute("posts", noPosts);

		blogPostAtomViewer.buildFeedMetadata(model, feed, request);

		assertThat(feed.getUpdated(), is(nullValue()));
	}

	private void buildPostsWithDate(int numberOfPosts, List<Post> posts) {
		for (int date = numberOfPosts; date > 0; date--) {
			calendar.set(2013, 6, date);
			Post post = PostBuilder.post().build();
			post.setCreatedDate(calendar.getTime());
			posts.add(post);
		}
	}

	@Test
	public void hasCorrectIdForEntry() throws Exception {
		calendar.set(2013, 6, 1);
		Post post = spy(PostBuilder.post().build());
		post.setCreatedDate(calendar.getTime());
		when(post.getId()).thenReturn(123L);

		model.addAttribute("posts", Arrays.asList(post));

		List<Entry> entries = blogPostAtomViewer.buildFeedEntries(model, request, mock(HttpServletResponse.class));

		Entry entry = entries.get(0);
		assertThat(entry.getId(), is("tag:springsource.org,2013-07-01:123"));
	}
}
