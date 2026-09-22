package com.atech.curso.m7.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RepoGitHub(
        String name,
        @JsonProperty("html_url") String url,
        @JsonProperty("private") boolean privado,
        String language,
        @JsonProperty("stargazers_count") int estrellas) {
}
