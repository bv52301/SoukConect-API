// common-domain/src/main/java/com/souk/common/port/SearchPort.java
package com.souk.common.port;

import java.util.*;

public interface SearchPort<T> {
    List<T> search(String query, int limit);
}
