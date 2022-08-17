/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.controlflow;

import lombok.Getter;
import lombok.Setter;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

import java.util.*;


public class ControlFlowVisualizer {

    public static void showCFG(ControlFlowSummary summary) {
        Set<ControlFlowNode> all = summary.getAllNodes();
        // map each node to its index in the list
        Map<ControlFlowNode, Integer> nodeToIndex = new HashMap<>();
        int index = 0;
        for (ControlFlowNode node : all) {
            nodeToIndex.put(node, index);
            index++;
        }

        ControlFlowNode cn = summary
                .getAllNodes()
                .stream()
                .filter(node -> node instanceof ControlFlowNode.BasicBlock)
                .findFirst().get();
        ControlFlowNode.BasicBlock basicBlock = (ControlFlowNode.BasicBlock) cn;

        PrintOutputCapture<Integer> capture
                = new PrintOutputCapture<>(0);
        JavaPrinter<Integer> printer = new JavaPrinter<>();
        printer.visit(basicBlock.getCommonBlock().getValue(), capture, basicBlock.getCommonBlock().getParentOrThrow());

        GraphShower shower = new GraphShower(
                nodeToIndex, capture.getOut());
        shower.runGraph();

        System.out.println("Graph shown.");


    }


    public static void createSVG(ControlFlowSummary summary) {
        Set<ControlFlowNode> all = summary.getAllNodes();
        // map each node to its index in the list
        Map<ControlFlowNode, Integer> nodeToIndex = new HashMap<>();
        int index = 0;
        for (ControlFlowNode node : all) {
            nodeToIndex.put(node, index);
            index++;
        }

        GraphStaticDisplay display = new GraphStaticDisplay(nodeToIndex);
        display.loadGraph();

        System.out.println("Graph displayed.");
    }




}
