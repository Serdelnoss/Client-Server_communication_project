package interfaces;

import java.util.List;

public interface DatabaseService {
    String getUrl(List<String []> list, int port, String dbName);
    String getDbName(List<String []> list);
    void startServer();
}
