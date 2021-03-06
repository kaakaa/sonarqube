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

package org.sonar.server.ui.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.PageDefinition;
import org.sonar.core.config.WebConstants;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.platform.PluginRepository;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.ws.WsActionTester;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class SettingsActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private Settings settings = new MapSettings();

  private WsActionTester ws;

  @Test
  public void empty() throws Exception {
    init();
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    executeAndVerify("empty.json");
  }

  @Test
  public void with_provisioning() throws Exception {
    init();
    userSessionRule.setGlobalPermissions(GlobalPermissions.PROVISIONING);

    executeAndVerify("with_provisioning.json");
  }

  @Test
  public void with_pages() throws Exception {
    init(createPages());
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    executeAndVerify("with_pages.json");
  }

  @Test
  public void with_update_center() throws Exception {
    init();
    settings.setProperty(WebConstants.SONAR_UPDATECENTER_ACTIVATE, true);
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    executeAndVerify("with_update_center.json");
  }

  @Test
  public void with_views_and_update_center_but_not_admin() throws Exception {
    init(createPages());
    settings.setProperty(WebConstants.SONAR_UPDATECENTER_ACTIVATE, true);

    executeAndVerify("empty.json");
  }

  private void init(Page... pages) {
    PluginRepository pluginRepository = mock(PluginRepository.class);
    when(pluginRepository.hasPlugin(anyString())).thenReturn(true);
    PageRepository pageRepository = new PageRepository(pluginRepository, new PageDefinition[] {context -> {
      for (Page page : pages) {
        context.addPage(page);
      }
    }});
    ws = new WsActionTester(new SettingsAction(pageRepository, settings, userSessionRule));
    pageRepository.start();
  }

  private void executeAndVerify(String json) {
    assertJson(ws.newRequest().execute().getInput()).isSimilarTo(getClass().getResource("SettingsActionTest/" + json));
  }

  private Page[] createPages() {
    Page firstPage = Page.builder("my_plugin/first_page").setName("First Page").setAdmin(true).build();
    Page secondPage = Page.builder("my_plugin/second_page").setName("Second Page").setAdmin(true).build();

    return new Page[] {firstPage, secondPage};
  }
}
