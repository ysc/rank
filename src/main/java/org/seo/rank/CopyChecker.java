/**
 *
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.seo.rank;

import org.seo.rank.model.Article;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文章抄袭检查
 * 比如我写了一篇文章：使用Java8实现自己的个性化搜索引擎
 * 我想知道有哪些网站转载了我的文章
 * 那么我可以通过搜索引擎来进行查询
 * @author 杨尚川
 */
public interface CopyChecker {
    /**
     * 返回结果中的Set里面的内容是抄袭的文章的URL
     * @param titles
     * @return
     */
    public Map<Article, Set<String>> check(List<Article> titles);
}
