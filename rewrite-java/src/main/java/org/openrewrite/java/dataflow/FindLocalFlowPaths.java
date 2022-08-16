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
package org.openrewrite.java.dataflow;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.dataflow.analysis.DataFlowGraph;
import org.openrewrite.java.dataflow.analysis.SinkFlow;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.JavaSourceFile;

import java.util.*;

import static java.util.Objects.requireNonNull;

@Incubating(since = "7.24.0")
@RequiredArgsConstructor
public class FindLocalFlowPaths extends JavaIsoVisitor<ExecutionContext> {
    private static final String FLOW_GRAPHS = "flowGraphs";
    private final LocalFlowSpec<?, ?> spec;

    @Override
    public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {
        getCursor().putMessage(FLOW_GRAPHS, new ArrayList<>());
        JavaSourceFile c = super.visitJavaSourceFile(cu, ctx);

        Set<Expression> flowSteps = Collections.newSetFromMap(new IdentityHashMap<>());
        List<SinkFlow<?, ?>> sinkFlows = getCursor().getMessage(FLOW_GRAPHS);
        for (SinkFlow<?, ?> flowGraph : requireNonNull(sinkFlows)) {
            for (List<Cursor> flow : flowGraph.getFlows()) {
                for (Cursor step : flow) {
                    flowSteps.add(step.getValue());
                }
            }
        }

        if (!flowSteps.isEmpty()) {
            doAfterVisit(new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public Expression visitExpression(Expression expression, ExecutionContext ctx) {
                    return flowSteps.contains(expression) ?
                            expression.withMarkers(expression.getMarkers().searchResult()) :
                            expression;
                }
            });
        }
        return c;
    }

    @Override
    public Expression visitExpression(Expression expression, ExecutionContext ctx) {
        Dataflow.startingAt(getCursor()).findSinks(spec).ifPresent(flow -> {
            if (flow.isNotEmpty()) {
                List<SinkFlow<?, ?>> flowGraphs = getCursor().getNearestMessage(FLOW_GRAPHS);
                assert flowGraphs != null;
                flowGraphs.add(flow);
                // BFS to find all reachable nodes
                DataFlowGraph g = new DataFlowGraph(flow);
                g.getFlows();

            }
        });
        return expression;
    }
}
