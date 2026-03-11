package com.saraasansor.api.revisionstandards.repository;

import com.saraasansor.api.revisionstandards.model.RevisionStandard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RevisionStandardsRepository {

    private static final RowMapper<RevisionStandard> ROW_MAPPER = (rs, rowNum) -> {
        RevisionStandard standard = new RevisionStandard();
        standard.setId(rs.getLong("id"));
        standard.setStandardCode(rs.getString("standard_code"));
        standard.setArticleNo(rs.getString("article_no"));
        standard.setDescription(rs.getString("description"));
        standard.setTagColor(rs.getString("tag_color"));
        standard.setPrice(rs.getBigDecimal("price"));
        standard.setSourceFileName(rs.getString("source_file_name"));
        standard.setSourceVersion(rs.getString("source_version"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            standard.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            standard.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return standard;
    };

    private final JdbcTemplate jdbcTemplate;

    public RevisionStandardsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> findIdByStandardAndArticleNo(String standardCode, String articleNo) {
        List<Long> ids = jdbcTemplate.query(
                """
                SELECT id
                FROM public.revision_standards
                WHERE standard_code = ? AND article_no = ?
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getLong("id"),
                standardCode,
                articleNo
        );
        return ids.stream().findFirst();
    }

    public int insert(RevisionStandard standard) {
        LocalDateTime now = LocalDateTime.now();
        return jdbcTemplate.update(
                """
                INSERT INTO public.revision_standards
                (standard_code, article_no, description, tag_color, price, source_file_name, source_version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                standard.getStandardCode(),
                standard.getArticleNo(),
                standard.getDescription(),
                standard.getTagColor(),
                standard.getPrice(),
                standard.getSourceFileName(),
                standard.getSourceVersion(),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );
    }

    public int update(RevisionStandard standard) {
        return jdbcTemplate.update(
                """
                UPDATE public.revision_standards
                SET description = ?,
                    tag_color = ?,
                    price = ?,
                    source_file_name = ?,
                    source_version = ?,
                    updated_at = ?
                WHERE standard_code = ? AND article_no = ?
                """,
                standard.getDescription(),
                standard.getTagColor(),
                standard.getPrice(),
                standard.getSourceFileName(),
                standard.getSourceVersion(),
                Timestamp.valueOf(LocalDateTime.now()),
                standard.getStandardCode(),
                standard.getArticleNo()
        );
    }

    public List<RevisionStandard> search(String query, int limit) {
        return jdbcTemplate.query(
                """
                SELECT id, article_no, description, standard_code, tag_color, price, source_file_name, source_version, created_at, updated_at
                FROM public.revision_standards
                WHERE article_no ILIKE ? OR description ILIKE ?
                ORDER BY
                    CASE
                        WHEN LOWER(article_no) = LOWER(?) THEN 0
                        WHEN article_no ILIKE ? THEN 1
                        ELSE 2
                    END,
                    article_no
                LIMIT ?
                """,
                ROW_MAPPER,
                query + "%",
                "%" + query + "%",
                query,
                query + "%",
                limit
        );
    }
}
