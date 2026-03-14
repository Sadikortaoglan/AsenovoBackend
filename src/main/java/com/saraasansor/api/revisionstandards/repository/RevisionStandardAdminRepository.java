package com.saraasansor.api.revisionstandards.repository;

import com.saraasansor.api.revisionstandards.model.RevisionStandard;
import com.saraasansor.api.revisionstandards.model.RevisionStandardSet;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RevisionStandardAdminRepository {

    private static final RowMapper<RevisionStandardSet> SET_ROW_MAPPER = (rs, rowNum) -> {
        RevisionStandardSet item = new RevisionStandardSet();
        item.setId(rs.getLong("id"));
        item.setStandardCode(rs.getString("standard_code"));
        item.setArticleCount(rs.getLong("article_count"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            item.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            item.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return item;
    };

    private static final RowMapper<RevisionStandard> ARTICLE_ROW_MAPPER = (rs, rowNum) -> {
        RevisionStandard standard = new RevisionStandard();
        standard.setId(rs.getLong("id"));
        standard.setStandardCode(rs.getString("standard_code"));
        standard.setArticleNo(rs.getString("article_no"));
        standard.setDescription(rs.getString("description"));
        standard.setTagColor(rs.getString("tag_color"));
        standard.setPrice(rs.getBigDecimal("price"));
        standard.setSourceFileName(rs.getString("source_file_name"));
        standard.setSourceVersion(rs.getString("source_version"));
        return standard;
    };

    private final JdbcTemplate jdbcTemplate;

    public RevisionStandardAdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureStandardSetExists(String standardCode) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                INSERT INTO public.revision_standard_sets (standard_code, created_at, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT (standard_code) DO UPDATE
                SET updated_at = EXCLUDED.updated_at
                """,
                standardCode,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );
    }

    public List<RevisionStandardSet> findStandardSets(String query, Pageable pageable) {
        String normalized = normalizeQuery(query);
        StringBuilder sql = new StringBuilder("""
                SELECT s.id,
                       s.standard_code,
                       s.created_at,
                       s.updated_at,
                       COUNT(rs.id) AS article_count
                FROM public.revision_standard_sets s
                LEFT JOIN public.revision_standards rs ON rs.standard_code = s.standard_code
                """);
        List<Object> params = new ArrayList<>();
        if (normalized != null) {
            sql.append(" WHERE s.standard_code ILIKE ? ");
            params.add("%" + normalized + "%");
        }
        sql.append("""
                GROUP BY s.id, s.standard_code, s.created_at, s.updated_at
                ORDER BY s.standard_code
                LIMIT ? OFFSET ?
                """);
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());
        return jdbcTemplate.query(sql.toString(), SET_ROW_MAPPER, params.toArray());
    }

    public long countStandardSets(String query) {
        String normalized = normalizeQuery(query);
        String sql = "SELECT COUNT(*) FROM public.revision_standard_sets";
        List<Object> params = new ArrayList<>();
        if (normalized != null) {
            sql += " WHERE standard_code ILIKE ?";
            params.add("%" + normalized + "%");
        }
        Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return count == null ? 0 : count;
    }

    public List<RevisionStandardSet> findStandardSets(String query) {
        String normalized = normalizeQuery(query);
        StringBuilder sql = new StringBuilder("""
                SELECT s.id,
                       s.standard_code,
                       s.created_at,
                       s.updated_at,
                       COUNT(rs.id) AS article_count
                FROM public.revision_standard_sets s
                LEFT JOIN public.revision_standards rs ON rs.standard_code = s.standard_code
                """);
        List<Object> params = new ArrayList<>();
        if (normalized != null) {
            sql.append(" WHERE s.standard_code ILIKE ? ");
            params.add("%" + normalized + "%");
        }
        sql.append("""
                GROUP BY s.id, s.standard_code, s.created_at, s.updated_at
                ORDER BY s.standard_code
                """);
        return jdbcTemplate.query(sql.toString(), SET_ROW_MAPPER, params.toArray());
    }

    public Optional<RevisionStandardSet> findStandardSetById(Long id) {
        List<RevisionStandardSet> result = jdbcTemplate.query(
                """
                SELECT s.id,
                       s.standard_code,
                       s.created_at,
                       s.updated_at,
                       COUNT(rs.id) AS article_count
                FROM public.revision_standard_sets s
                LEFT JOIN public.revision_standards rs ON rs.standard_code = s.standard_code
                WHERE s.id = ?
                GROUP BY s.id, s.standard_code, s.created_at, s.updated_at
                """,
                SET_ROW_MAPPER,
                id
        );
        return result.stream().findFirst();
    }

    public Optional<RevisionStandardSet> findStandardSetByCode(String standardCode) {
        List<RevisionStandardSet> result = jdbcTemplate.query(
                """
                SELECT s.id,
                       s.standard_code,
                       s.created_at,
                       s.updated_at,
                       COUNT(rs.id) AS article_count
                FROM public.revision_standard_sets s
                LEFT JOIN public.revision_standards rs ON rs.standard_code = s.standard_code
                WHERE s.standard_code = ?
                GROUP BY s.id, s.standard_code, s.created_at, s.updated_at
                """,
                SET_ROW_MAPPER,
                standardCode
        );
        return result.stream().findFirst();
    }

    public Optional<String> findStandardCodeById(Long id) {
        List<String> result = jdbcTemplate.query(
                "SELECT standard_code FROM public.revision_standard_sets WHERE id = ?",
                (rs, rowNum) -> rs.getString("standard_code"),
                id
        );
        return result.stream().findFirst();
    }

    public Long createStandardSet(String standardCode) {
        LocalDateTime now = LocalDateTime.now();
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO public.revision_standard_sets (standard_code, created_at, updated_at)
                VALUES (?, ?, ?)
                RETURNING id
                """,
                Long.class,
                standardCode,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );
    }

    public void updateStandardSet(Long id, String newStandardCode) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        String currentCode = jdbcTemplate.queryForObject(
                "SELECT standard_code FROM public.revision_standard_sets WHERE id = ?",
                String.class,
                id
        );
        jdbcTemplate.update(
                "UPDATE public.revision_standard_sets SET standard_code = ?, updated_at = ? WHERE id = ?",
                newStandardCode,
                now,
                id
        );
        jdbcTemplate.update(
                "UPDATE public.revision_standards SET standard_code = ?, updated_at = ? WHERE standard_code = ?",
                newStandardCode,
                now,
                currentCode
        );
    }

    public void deleteStandardSet(Long id) {
        String standardCode = jdbcTemplate.queryForObject(
                "SELECT standard_code FROM public.revision_standard_sets WHERE id = ?",
                String.class,
                id
        );
        jdbcTemplate.update("DELETE FROM public.revision_standards WHERE standard_code = ?", standardCode);
        jdbcTemplate.update("DELETE FROM public.revision_standard_sets WHERE id = ?", id);
    }

    public List<RevisionStandard> findArticlesByStandardSetId(Long standardSetId,
                                                              String query,
                                                              String tagColor,
                                                              BigDecimal minPrice,
                                                              BigDecimal maxPrice,
                                                              Pageable pageable) {
        String standardCode = jdbcTemplate.queryForObject(
                "SELECT standard_code FROM public.revision_standard_sets WHERE id = ?",
                String.class,
                standardSetId
        );
        String normalized = normalizeQuery(query);
        StringBuilder sql = new StringBuilder("""
                SELECT id, standard_code, article_no, description, tag_color, price, source_file_name, source_version
                FROM public.revision_standards
                WHERE standard_code = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(standardCode);
        if (normalized != null) {
            sql.append(" AND (article_no ILIKE ? OR description ILIKE ?) ");
            params.add(normalized + "%");
            params.add("%" + normalized + "%");
        }
        if (tagColor != null) {
            sql.append(" AND UPPER(tag_color) = UPPER(?) ");
            params.add(tagColor);
        }
        if (minPrice != null) {
            sql.append(" AND price >= ? ");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND price <= ? ");
            params.add(maxPrice);
        }
        sql.append(" ORDER BY article_no LIMIT ? OFFSET ? ");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());
        return jdbcTemplate.query(sql.toString(), ARTICLE_ROW_MAPPER, params.toArray());
    }

    public long countArticlesByStandardSetId(Long standardSetId,
                                             String query,
                                             String tagColor,
                                             BigDecimal minPrice,
                                             BigDecimal maxPrice) {
        String standardCode = jdbcTemplate.queryForObject(
                "SELECT standard_code FROM public.revision_standard_sets WHERE id = ?",
                String.class,
                standardSetId
        );
        String normalized = normalizeQuery(query);
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM public.revision_standards
                WHERE standard_code = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(standardCode);
        if (normalized != null) {
            sql.append(" AND (article_no ILIKE ? OR description ILIKE ?) ");
            params.add(normalized + "%");
            params.add("%" + normalized + "%");
        }
        if (tagColor != null) {
            sql.append(" AND UPPER(tag_color) = UPPER(?) ");
            params.add(tagColor);
        }
        if (minPrice != null) {
            sql.append(" AND price >= ? ");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND price <= ? ");
            params.add(maxPrice);
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count == null ? 0 : count;
    }

    public List<RevisionStandard> findArticlesByStandardSetId(Long standardSetId,
                                                              String query,
                                                              String tagColor,
                                                              BigDecimal minPrice,
                                                              BigDecimal maxPrice) {
        String standardCode = jdbcTemplate.queryForObject(
                "SELECT standard_code FROM public.revision_standard_sets WHERE id = ?",
                String.class,
                standardSetId
        );
        String normalized = normalizeQuery(query);
        StringBuilder sql = new StringBuilder("""
                SELECT id, standard_code, article_no, description, tag_color, price, source_file_name, source_version
                FROM public.revision_standards
                WHERE standard_code = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(standardCode);
        if (normalized != null) {
            sql.append(" AND (article_no ILIKE ? OR description ILIKE ?) ");
            params.add(normalized + "%");
            params.add("%" + normalized + "%");
        }
        if (tagColor != null) {
            sql.append(" AND UPPER(tag_color) = UPPER(?) ");
            params.add(tagColor);
        }
        if (minPrice != null) {
            sql.append(" AND price >= ? ");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND price <= ? ");
            params.add(maxPrice);
        }
        sql.append(" ORDER BY article_no ");
        return jdbcTemplate.query(sql.toString(), ARTICLE_ROW_MAPPER, params.toArray());
    }

    public Optional<RevisionStandard> findArticleById(Long articleId) {
        List<RevisionStandard> items = jdbcTemplate.query(
                """
                SELECT id, standard_code, article_no, description, tag_color, price, source_file_name, source_version
                FROM public.revision_standards
                WHERE id = ?
                """,
                ARTICLE_ROW_MAPPER,
                articleId
        );
        return items.stream().findFirst();
    }

    public Long createArticle(Long standardSetId, RevisionStandard article) {
        String standardCode = jdbcTemplate.queryForObject(
                "SELECT standard_code FROM public.revision_standard_sets WHERE id = ?",
                String.class,
                standardSetId
        );
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO public.revision_standards
                (standard_code, article_no, description, tag_color, price, source_file_name, source_version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                standardCode,
                article.getArticleNo(),
                article.getDescription(),
                article.getTagColor(),
                defaultPrice(article.getPrice()),
                article.getSourceFileName(),
                article.getSourceVersion(),
                now,
                now
        );
    }

    public void updateArticle(Long articleId, RevisionStandard article) {
        jdbcTemplate.update(
                """
                UPDATE public.revision_standards
                SET article_no = ?,
                    description = ?,
                    tag_color = ?,
                    price = ?,
                    updated_at = ?
                WHERE id = ?
                """,
                article.getArticleNo(),
                article.getDescription(),
                article.getTagColor(),
                defaultPrice(article.getPrice()),
                Timestamp.valueOf(LocalDateTime.now()),
                articleId
        );
    }

    public void deleteArticle(Long articleId) {
        jdbcTemplate.update("DELETE FROM public.revision_standards WHERE id = ?", articleId);
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }

    private BigDecimal defaultPrice(BigDecimal price) {
        return price == null ? BigDecimal.ZERO : price;
    }
}
