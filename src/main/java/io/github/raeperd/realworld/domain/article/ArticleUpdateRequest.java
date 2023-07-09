package io.github.raeperd.realworld.domain.article;

import lombok.Builder;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@Builder
public class ArticleUpdateRequest {

    private final ArticleTitle titleToUpdate;
    private final String descriptionToUpdate;
    private final String bodyToUpdate;

    // optional은 코틀린에서 nullability로 대체할 수 있다.
    Optional<ArticleTitle> getTitleToUpdate() {
        return ofNullable(titleToUpdate);
    }

    Optional<String> getDescriptionToUpdate() {
        return ofNullable(descriptionToUpdate);
    }

    Optional<String> getBodyToUpdate() {
        return ofNullable(bodyToUpdate);
    }
}