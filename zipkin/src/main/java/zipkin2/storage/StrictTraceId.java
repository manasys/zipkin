/*
 * Copyright 2015-2018 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin2.storage;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import zipkin2.Call;
import zipkin2.Span;

/**
 * Storage implementation often need to re-check query results when {@link
 * StorageComponent.Builder#strictTraceId(boolean) strict trace ID} is disabled.
 */
public final class StrictTraceId {

  public static Call.Mapper<List<Span>, List<Span>> filterSpans(String traceId) {
    return new FilterSpans(traceId);
  }

  /** Filters the mutable input based on the query */
  public static Call.Mapper<List<List<Span>>, List<List<Span>>> filterTraces(QueryRequest request) {
    return new FilterTracesByQuery(request);
  }

  /** Filters the mutable input based on the query */
  public static Call.Mapper<List<List<Span>>, List<List<Span>>> filterTraces(Collection<String> traceIds) {
    return new FilterTracesByIds(traceIds);
  }

  static final class FilterTracesByQuery implements Call.Mapper<List<List<Span>>, List<List<Span>>> {

    final QueryRequest request;

    FilterTracesByQuery(QueryRequest request) {
      this.request = request;
    }

    @Override
    public List<List<Span>> map(List<List<Span>> input) {
      Iterator<List<Span>> i = input.iterator();
      while (i.hasNext()) { // Not using removeIf as that's java 8+
        List<Span> next = i.next();
        if (!request.test(next)) i.remove();
      }
      return input;
    }

    @Override
    public String toString() {
      return "FilterTracesByQuery{request=" + request + "}";
    }
  }

  static final class FilterTracesByIds implements Call.Mapper<List<List<Span>>, List<List<Span>>> {

    final Set<String> traceIds;

    FilterTracesByIds(Collection<String> unsanitizedIds) {
      traceIds = new LinkedHashSet<>();
      for (String traceId : unsanitizedIds) {
        traceIds.add(Span.normalizeTraceId(traceId));
      }
    }

    @Override
    public List<List<Span>> map(List<List<Span>> input) {
      Iterator<List<Span>> i = input.iterator();
      while (i.hasNext()) { // Not using removeIf as that's java 8+
        List<Span> next = i.next();
        if (!traceIds.contains(next.get(0).traceId())) {
          i.remove();
        }
      }
      return input;
    }

    @Override
    public String toString() {
      return "FilterTracesByIds{traceIds=" + traceIds + "}";
    }
  }

  static final class FilterSpans implements Call.Mapper<List<Span>, List<Span>> {

    final String traceId;

    FilterSpans(String traceId) {
      this.traceId = traceId;
    }

    @Override
    public List<Span> map(List<Span> input) {
      Iterator<Span> i = input.iterator();
      while (i.hasNext()) { // Not using removeIf as that's java 8+
        Span next = i.next();
        if (!next.traceId().equals(traceId)) i.remove();
      }
      return input;
    }

    @Override
    public String toString() {
      return "FilterSpans{traceId=" + traceId + "}";
    }
  }

  StrictTraceId() {
  }
}