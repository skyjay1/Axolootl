/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.util;

import axolootl.AxRegistry;
import axolootl.data.aquarium_tab.IAquariumTab;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import net.minecraftforge.fml.loading.toposort.TopologicalSort;

import java.util.List;

public final class ControllerTabSorter {

    /**
     * @return a sorted list of aquarium tabs
     */
    public static List<IAquariumTab> recalculateSortedTabs() {
        // create edges
        final Multimap<IAquariumTab, IAquariumTab> edges = HashMultimap.create();
        // create graph
        final MutableGraph<IAquariumTab> graph = GraphBuilder.directed().nodeOrder(ElementOrder.<IAquariumTab>insertion()).build();
        // iterate tab registry
        for(IAquariumTab tab : AxRegistry.AQUARIUM_TABS_SUPPLIER.get()) {
            // add tab node
            graph.addNode(tab);
            // add edges for tabs before this one
            for(IAquariumTab tabBefore : tab.getBeforeTabs()) {
                edges.put(tabBefore, tab);
            }
            // add edges for tabs after this one
            for(IAquariumTab tabAfter : tab.getAfterTabs()) {
                edges.put(tab, tabAfter);
            }
        }
        // add all edges to graph
        edges.forEach(graph::putEdge);
        // sort the graph
        return TopologicalSort.topologicalSort(graph, null);
    }
}
