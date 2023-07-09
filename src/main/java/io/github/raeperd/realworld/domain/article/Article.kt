package io.github.raeperd.realworld.domain.article

import io.github.raeperd.realworld.domain.article.comment.Comment
import io.github.raeperd.realworld.domain.user.User
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*
import javax.persistence.*
import kotlin.NoSuchElementException

@Table(name = "articles")
@EntityListeners(AuditingEntityListener::class)
@Entity
data class Article(
    // JPA 결합되어 있는 엔티티 클래스
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private val id: Long? = null,

    @JoinColumn(name = "author_id", referencedColumnName = "id", nullable = false)
    @ManyToOne(fetch = FetchType.EAGER)
    val author: User? = null,

    @Embedded // ArticleContents가 Article에 포함된다. 단일 데이터베이스 테이블에 매핑된다.
    val contents: ArticleContents? = null,

    @JvmField
    @Column(name = "created_at")
    @CreatedDate
    val createdAt: Instant? = null,

    @JvmField
    @Column(name = "updated_at")
    @LastModifiedDate
    val updatedAt: Instant? = null,

    @JvmField
    @JoinTable(
        name = "article_favorites",
        joinColumns = [JoinColumn(name = "article_id", referencedColumnName = "id", nullable = false)],
        inverseJoinColumns = [JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)]
    ) // TODO("다대다 관계를 1:다 다:1 로 해소)
    @ManyToMany(fetch = FetchType.EAGER, cascade = [CascadeType.PERSIST])
    var userFavorited: MutableSet<User> = HashSet(),

    @OneToMany(mappedBy = "article", cascade = [CascadeType.PERSIST, CascadeType.REMOVE])
    private val comments: MutableSet<Comment> = HashSet()
) {
    fun afterUserFavoritesArticle(user: User): Article { // 사용자가 게시글을 좋아요 했을 때 호출
        userFavorited.add(user)
        return this
    }

    fun afterUserUnFavoritesArticle(user: User): Article {
        userFavorited.remove(user)
        return this
    }

    fun addComment(author: User?, body: String?): Set<Comment> {
        comments.add(Comment(this, author, body)) // 상태
        return comments
    }

    fun removeCommentByUser(user: User, commentId: Long) { // 책임 분리 필요
        if (user != author || user != getCommentsToDelete(commentId).author) {
            throw IllegalAccessError("Not authorized to delete comment")
        }
        comments.remove(getCommentsToDelete(commentId))
    }

    private fun getCommentsToDelete(commentId: Long): Comment =
        comments.first { it.id == commentId }.let { throw NoSuchElementException() }

    fun updateArticle(updateRequest: ArticleUpdateRequest?) {
        contents!!.updateArticleContentsIfPresent(updateRequest)
    }

    val favoritedCount: Int = userFavorited.size

    fun getComments(): Set<Comment> {
        return comments
    }
}
