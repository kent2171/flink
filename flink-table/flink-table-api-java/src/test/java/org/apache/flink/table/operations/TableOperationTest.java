/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.operations;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.expressions.ApiExpressionUtils;
import org.apache.flink.table.expressions.BuiltInFunctionDefinitions;
import org.apache.flink.table.expressions.CallExpression;
import org.apache.flink.table.expressions.FieldReferenceExpression;
import org.apache.flink.table.typeutils.TimeIntervalTypeInfo;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * Tests for describing {@link TableOperation}s.
 */
public class TableOperationTest {

	@Test
	public void testSummaryString() {
		TableSchema schema = TableSchema.builder().field("a", DataTypes.INT()).build();

		ProjectTableOperation tableOperation = new ProjectTableOperation(
			Collections.singletonList(new FieldReferenceExpression("a", Types.INT, 0, 0)),
			new CatalogTableOperation(
				Arrays.asList("cat1", "db1", "tab1"),
				schema), schema);

		SetTableOperation unionTableOperation = new SetTableOperation(
			tableOperation,
			tableOperation,
			SetTableOperation.SetTableOperationType.UNION,
			true);

		assertEquals("Union: (all: [true])\n" +
			"    Project: (projections: [a])\n" +
			"        CatalogTable: (path: [cat1, db1, tab1], fields: [a])\n" +
			"    Project: (projections: [a])\n" +
			"        CatalogTable: (path: [cat1, db1, tab1], fields: [a])",
			unionTableOperation.asSummaryString());
	}

	@Test
	public void testWindowAggregationSummaryString() {
		TableSchema schema = TableSchema.builder().field("a", DataTypes.INT()).build();
		FieldReferenceExpression field = new FieldReferenceExpression("a", Types.INT, 0, 0);
		WindowAggregateTableOperation tableOperation = new WindowAggregateTableOperation(
			Collections.singletonList(field),
			Collections.singletonList(new CallExpression(BuiltInFunctionDefinitions.SUM,
				Collections.singletonList(field))),
			Collections.emptyList(),
			WindowAggregateTableOperation.ResolvedGroupWindow.sessionWindow("w", field, ApiExpressionUtils.valueLiteral(
				10,
				TimeIntervalTypeInfo.INTERVAL_MILLIS)),
			new CatalogTableOperation(
				Arrays.asList("cat1", "db1", "tab1"),
				schema),
			schema
		);

		DistinctTableOperation distinctTableOperation = new DistinctTableOperation(tableOperation);

		assertEquals(
			"Distinct:\n" +
			"    WindowAggregate: (group: [a], agg: [sum(a)], windowProperties: []," +
				" window: [SessionWindow(field: [a], gap: [10.millis])])\n" +
				"        CatalogTable: (path: [cat1, db1, tab1], fields: [a])",
			distinctTableOperation.asSummaryString());
	}

	@Test
	public void testIndentation() {

		String input =
			"firstLevel\n" +
			"    secondLevel0\n" +
			"        thirdLevel0\n" +
			"    secondLevel1\n" +
			"        thirdLevel1";

		String indentedInput = TableOperationUtils.indent(input);

		assertEquals(
			"\n" +
			"    firstLevel\n" +
			"        secondLevel0\n" +
			"            thirdLevel0\n" +
			"        secondLevel1\n" +
			"            thirdLevel1",
			indentedInput);
	}
}
