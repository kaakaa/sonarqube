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
package org.sonar.server.component.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.es.EsTester;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newProjectDto;

public class SuggestionsActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public EsTester es = new EsTester(new ComponentIndexDefinition(new MapSettings()));

  private ComponentIndex index;
  private ComponentIndexer indexer;
  private SuggestionsAction action;

  @Before
  public void setUp() {
    index = new ComponentIndex(es.client());
    indexer = new ComponentIndexer(db.getDbClient(), es.client());
    action = new SuggestionsAction(db.getDbClient(), index);
  }

  @Test
  public void exact_match_in_one_qualifier() {
    ComponentDto dto = newProjectDto("project-uuid").setId(42l);
    db.getDbClient().componentDao().insert(db.getSession(), dto);
    db.commit();

    indexer.index();

    SuggestionsWsResponse response = action.doHandle(dto.getKey());

    // assert match in qualifier "TRK"
    assertThat(
      response.getResultsList()
        .stream()
        .map(q -> q.getQ())
        .collect(Collectors.toList()))
          .containsExactly(Qualifiers.PROJECT);

    // assert correct id to be found
    assertThat(
      response.getResultsList()
        .stream()
        .flatMap(q -> q.getItemsList().stream())
        .map(c -> c.getId())
        .collect(Collectors.toList()))
          .containsExactly(Long.toString(dto.getId()));
  }
}
