/*
   Copyright 2026 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.wearefrank.ladybug;

import java.util.*;

class SpanGraph {
    private Map<String, List<String>> adjList;

    public SpanGraph(String root) {
        adjList = new HashMap<>();
        adjList.put(root, new ArrayList<>());
    }

    public void addEdge(String parent, String child) {
        adjList.putIfAbsent(parent, new ArrayList<>());
        adjList.putIfAbsent(parent, new ArrayList<>());
        adjList.get(parent).add(child);
        adjList.get(parent).add(child);
    }

    public void dfs(String root) {
        Set<String> visited = new HashSet<>();
        dfsRecursive(root, visited);
    }

    private void dfsRecursive(String node, Set<String> visited) {
        System.out.println("startpoint for: " + node);
        visited.add(node);

        if (adjList.containsKey(node)) {
            for (String neighbor : adjList.get(node)) {
                if (!visited.contains(neighbor)) {
                    dfsRecursive(neighbor, visited);
                }
            }
        }

        System.out.println("3 endpoint for: " + node);
    }
}