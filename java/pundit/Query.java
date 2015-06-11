package pundit;

import clojure.lang.*;
import java.util.*;

public class Query implements Seqable {
  private String parseClass;
  private IFn loader;
  private IPersistentMap auth;
  private IPersistentMap query;

  public Query(String parseClass, IFn loader, IPersistentMap auth, IPersistentMap query) {
    this.parseClass = parseClass;
    this.loader = loader;
    this.auth = auth;
    this.query = (query == null ? PersistentArrayMap.EMPTY : query);
  }

  public Query(String parseClass, IFn loader, IPersistentMap auth) {
    this.parseClass = parseClass;
    this.loader = loader;
    this.auth = auth;
    this.query = PersistentArrayMap.EMPTY;
  }

  public Query add(IPersistentMap more, IFn merge) {
    // We pass the auth map so that a query can be realized
    // outside of a dynamic binding.
    return new Query(
      parseClass,
      loader,
      auth,
      (IPersistentMap) merge.invoke(query, more));
  }

  public String getParseClass() {
    return parseClass;
  }

  public IPersistentMap getQuery() {
    return query;
  }

  public ISeq seq() {
    return ((ISeq) loader.invoke(parseClass, auth, query)).seq();
  }

  public String toString() {
    return String.format("%s %s", parseClass, query);
  }
}
