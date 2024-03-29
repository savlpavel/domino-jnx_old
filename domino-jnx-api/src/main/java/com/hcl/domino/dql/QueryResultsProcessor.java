/*
 * ==========================================================================
 * Copyright (C) 2019-2021 HCL America, Inc. ( http://www.hcl.com/ )
 *                            All rights reserved.
 * ==========================================================================
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may
 * not use this file except in compliance with the License.  You may obtain a
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the  specific language  governing permissions  and limitations
 * under the License.
 * ==========================================================================
 */
package com.hcl.domino.dql;

import java.io.Reader;
import java.util.Collection;
import java.util.Set;

import com.hcl.domino.data.Database;
import com.hcl.domino.data.IDTable;

/**
 * Aggregates, computes, sorts, and formats collections of documents across any
 * set of Domino databases.
 */
public interface QueryResultsProcessor {

  public enum Categorized {
    TRUE,
    FALSE
  }

  public enum Hidden {
    TRUE,
    FALSE
  }

  /**
   * Results processing options
   */
  public enum QRPOptions {
    /** returns the UNID instead of noteid */
    RETURN_UNID,
    /** returns the replicaid instead of db filepath */
    RETURN_REPLICAID
  }

  public enum SortOrder {
    UNORDERED(0),
    ASCENDING(1),
    DESCENDING(2);

    private final int m_value;

    SortOrder(final int value) {
      this.m_value = value;
    }

    public int getValue() {
      return this.m_value;
    }
  }

  /**
   * Convenience method to add an unsorted column to the processor
   *
   * @param name programmatic column name
   */
  void addColumn(String name);

  /**
   * Provides Domino formula language to override the data used to generate values
   * for a
   * particular sort column and an input collection or set of collections. Since
   * input collections
   * can be created from different databases, design differences can be adjusted
   * using addFormula
   * to have homogenous values in the output.
   *
   * @param formula     Formula language string to be evaluated in order to supply
   *                    the values for a sort column
   * @param columnname  String value responding the programmatic name of the sort
   *                    column whose values are to be generated using the formula
   *                    language supplied
   * @param resultsname Used to specify the input collection names to which will
   *                    use the formula language to generate sort column values.
   *                    Wildcards may be specified to map to multiple input
   *                    collection names.
   */
  void addFormula(String formula, String columnname, String resultsname);

  /**
   * Adds note ids from a database to the query results processor
   *
   * @param parentDb    database that contains the documents
   * @param noteIds     note ids, e.g. an {@link IDTable}
   * @param resultsname A unique name (to the QueryResultsProcessor instance) of
   *                    the input. This name is used in returned entries when the
   *                    origin of results is desired and in addFormula method
   *                    calls to override the data used to create sorted column
   *                    values.
   */
  void addNoteIds(Database parentDb, Collection<Integer> noteIds, String resultsname);

  /**
   * Creates a single column of values to be returned when
   * {@link QueryResultsProcessor} execute
   * is performed. Values for the column can be generated from a field, or a
   * formula. Sorting order,
   * categorization and hidden attributes determine the returned stream of results
   * entries.<br>
   * Sort columns span all input result sets and databases taking part in the
   * {@link QueryResultsProcessor}.
   *
   * @param name          The unique (within a QueryResultsProcessor instance)
   *                      programmatic name of the column. If there is no override
   *                      using the addFormula method call, the name provided will
   *                      be treated as a field name in each database involved in
   *                      the QueryResultsProcessor object. In JSON output, the
   *                      name value is used the element name for each returned
   *                      entry.<br>
   *                      Values in the name field can specify aggregate
   *                      functions. These functions require categorized columns
   *                      and return computed numerical values across sets of
   *                      results within a category. For aggregate functions
   *                      requiring a name as an argument, the name can be
   *                      overridden just as for a name without an aggregate
   *                      function. For more information on aggregate functions,
   *                      see the description of the isCategorized parameter.
   * @param title         The display title of the column. Used only in generated
   *                      views, the title is the UI column header.
   * @param sortorder     A constant to indicate how the column should be sorted.
   *                      Values are sorted case and accent insensitively by
   *                      default. Multiple sort columns can have sort orders, and
   *                      each order specified is sequentially applied in the
   *                      order of addSortColumn calls. Field lists
   *                      (multiply-occurring field values) are compared processed
   *                      using field occurrences from first to last sequentially.
   * @param ishidden      Sorts by a column value without returning the value. If
   *                      true, the column cannot have a sort order of
   *                      {@link SortOrder#UNORDERED} and cannot have an
   *                      iscategorized value of true.
   * @param iscategorized Categorized columns have a single entry returned for
   *                      each unique value with entries containing that value
   *                      nested under it. In JSON results, these nested entries
   *                      are represented in arrays under each categorized unique
   *                      value.<br>
   *                      Multiply-occurring fields (i.e. lists) are not allowed
   *                      to be categorized.<br>
   *                      A categorized column creates values for any preceding
   *                      uncategorized column in addition to the categorized
   *                      column. Categorized columns can nest to create
   *                      subcategories.
   */
  void addSortColumn(String name, String title, SortOrder sortorder, Hidden ishidden, Categorized iscategorized);

  /**
   * Processes the input collections in the manner specified by the Sort Columns,
   * overriding
   * field values with formulas specified via addFormula calls, and returns JSON
   * output.<br>
   * <br>
   * The JSON syntax produced by {@link QueryResultsProcessor} execution conforms
   * to JSON RFC 8259.<br>
   * All results are output under the “StreamedResults” top element key. For
   * categorized results,
   * all nested details are output under the “Documents” key.<br>
   * Special keys “@nid” for NoteID and “@DbPath” are output so results can be
   * acted upon on a document basis.<br>
   * Fields that are lists on documents (multiply-occurring) are output as JSON
   * arrays of like type.
   *
   * @param appendable the execution result is written in JSON format into this
   *                   {@link Appendable} in small chunks
   * @param options    options to tweak the JSON output or null/empty for default
   *                   format
   */
  void executeToJSON(Appendable appendable, Set<QRPOptions> options);

  /**
   * Processes the input collections in the manner specified by the Sort Columns,
   * overriding
   * field values with formulas specified via addFormula calls, and returns JSON
   * output.<br>
   * <br>
   * The JSON syntax produced by {@link QueryResultsProcessor} execution conforms
   * to JSON RFC 8259.<br>
   * All results are output under the “StreamedResults” top element key. For
   * categorized results,
   * all nested details are output under the “Documents” key.<br>
   * Special keys “@nid” for NoteID and “@DbPath” are output so results can be
   * acted upon on a document basis.<br>
   * Fields that are lists on documents (multiply-occurring) are output as JSON
   * arrays of like type.
   *
   * @return reader to receive the the JSON data
   * @param options options to tweak the JSON output or null/empty for default
   *                format
   */
  Reader executeToJSON(Set<QRPOptions> options);

}
