/*************************************************************************************
 * Copyright 2006, 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.

 *************************************************************************************/

package org.sakaiproject.wall.api;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
public class QueryBean {

    public String wallId = "";
    public String siteId = "";
    public String embedder = "";
    public boolean isUserSite = false;
    public List<String> fromIds = new ArrayList<String>();
    public String callerId = "";
}
