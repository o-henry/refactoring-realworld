package io.github.raeperd.realworld.domain.article;

import io.github.raeperd.realworld.domain.article.comment.Comment;
import io.github.raeperd.realworld.domain.user.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.FetchType.EAGER;
import static javax.persistence.GenerationType.IDENTITY;


@Table(name = "articles")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class Article { // JPA 결합되어 있는 엔티티 클래스

    @GeneratedValue(strategy = IDENTITY)
    @Id
    private Long id;

    @JoinColumn(name = "author_id", referencedColumnName = "id", nullable = false)
    @ManyToOne(fetch = EAGER)
    private User author;

    @Embedded // ArticleContents가 Article에 포함된다. 단일 데이터베이스 테이블에 매핑된다.
    private ArticleContents contents;

    @Column(name = "created_at")
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    @JoinTable(name = "article_favorites",
            joinColumns = @JoinColumn(name = "article_id", referencedColumnName = "id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false))
    // TODO("다대다 관계를 1:다 다:1 로 해소)
    @ManyToMany(fetch = EAGER, cascade = PERSIST)
    private Set<User> userFavorited = new HashSet<>();

    @OneToMany(mappedBy = "article", cascade = {PERSIST, REMOVE})
    private Set<Comment> comments = new HashSet<>();

    @Transient // 데이터베이스에 매핑되지 않는다.
    private boolean favorited = false; // 안티 패턴

    // 생성자
    public Article(User author, ArticleContents contents) {
        this.author = author;
        this.contents = contents;
    }

    protected Article() {
    }


    public Article afterUserFavoritesArticle(User user) { // 사용자가 게시글을 좋아요 했을 때 호출
        userFavorited.add(user); // 사용자가 게시글을 좋아요 했음을 추가
        return updateFavoriteByUser(user); // 게시글의 좋아요 상태를 업데이트
    }

    public Article afterUserUnFavoritesArticle(User user) {
        userFavorited.remove(user);
        return updateFavoriteByUser(user);
    }

    // updateFavoriteByUser(User user): 사용자에 따른 Article의 "favorite" 상태를 업데이트하는 메서드입니다. userFavorited 세트가 사용자를 포함하고 있는지 확인하고, 이에 따라 favorited 필드의 값을 업데이트합니다.
    public Article updateFavoriteByUser(User user) { // 사용자가 favorite을 누르면 article에서 favorite을 추가한다.
        favorited = userFavorited.contains(user); // boolean
        return this; // 현재 인스턴스를 반환한다.
    }


    public Comment addComment(User author, String body) { // 매개변수 이름 변경
        final var commentToAdd = new Comment(this, author, body);
        comments.add(commentToAdd);
        return commentToAdd;
    }

    public void removeCommentByUser(User user, long commentId) { // 책임 분리 필요
        final var commentsToDelete = comments.stream()
                .filter(comment -> comment.getId().equals(commentId))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
        if (!user.equals(author) || !user.equals(commentsToDelete.getAuthor())) {
            throw new IllegalAccessError("Not authorized to delete comment");
        }
        comments.remove(commentsToDelete);
    }

    public void updateArticle(ArticleUpdateRequest updateRequest) {
        contents.updateArticleContentsIfPresent(updateRequest);
    }

    //** boilerplate code
    public User getAuthor() {
        return author;
    }

    public ArticleContents getContents() {
        return contents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getFavoritedCount() {
        return userFavorited.size();
    }

    public boolean isFavorited() {
        return favorited;
    }

    public Set<Comment> getComments() {
        return comments;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var article = (Article) o;
        return author.equals(article.author) && contents.getTitle().equals(article.contents.getTitle());
    }

    @Override
    public int hashCode() {
        return Objects.hash(author, contents.getTitle());
    }
    //** boilerplate code
}
