/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.component.index;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.sonar.core.util.stream.Collectors;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_KEY;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_COMPONENTS;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;

public class ComponentIndex extends BaseIndex {

  /** Maximum length of ngrams (as defined in our elastic search properties). */
  private static final int MAXIMUM_NGRAM_LENGTH = 15;

  public ComponentIndex(EsClient client) {
    super(client);
  }

  public List<String> search(ComponentIndexQuery query) {
    SearchRequestBuilder request = getClient()
      .prepareSearch(INDEX_COMPONENTS)
      .setTypes(TYPE_COMPONENT)
      .setFetchSource(false);

    query.getLimit().ifPresent(request::setSize);

    request.setQuery(createQuery(query));

    return Arrays.stream(request.get().getHits().hits())
      .map(SearchHit::getId)
      .collect(Collectors.toList());
  }

  private static QueryBuilder createQuery(ComponentIndexQuery query) {
    BoolQueryBuilder esQuery = boolQuery();
    query.getQualifier().ifPresent(q -> esQuery.filter(termQuery(FIELD_QUALIFIER, q)));

    // We will truncate the search to the maximum length of nGrams in the index.
    // Otherwise the search would for sure not find any results.
    String truncatedQuery = StringUtils.left(query.getQuery(), MAXIMUM_NGRAM_LENGTH);

    return esQuery
      .should(matchQuery(FIELD_NAME + "." + SEARCH_PARTIAL_SUFFIX, truncatedQuery))
      .should(matchQuery(FIELD_KEY + "." + SORT_SUFFIX, query.getQuery()).boost(3f));
  }
}
