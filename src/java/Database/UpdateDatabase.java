package Database;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.json.*;
import pojo.MarketOrder;

public class UpdateDatabase {

    public static String result;

    public static void jsonFromUrl() throws IOException, JsonException {
        boolean hasData = true;
        int page_number = 0;

        String url = "https://esi.tech.ccp.is/latest/markets/10000002/orders/?datasource=tranquility&page=";
        List<MarketOrder> marketOrderList = new ArrayList<>();

        while (hasData) {
            page_number++;
            InputStream is = new URL(url + page_number).openStream();

            System.out.println(is.available());

            if (is.available() > 2) {
                JsonReader jreader = Json.createReader(is);
                JsonArray jsonArray = jreader.readArray();

                try {
                    for (int i = 0; i < jsonArray.size(); i++) {
                        MarketOrder marketOrder = new MarketOrder();
                        JsonObject jObject = jsonArray.getJsonObject(i);

                        marketOrder.setDuration(jObject.getInt("duration"));
                        marketOrder.setIs_buy_order(jObject.getBoolean("is_buy_order"));
                        marketOrder.setIssued(jObject.getString("issued"));
                        marketOrder.setLocation_id(jObject.getInt("location_id"));
                        marketOrder.setMin_volume(jObject.getInt("min_volume"));
                        marketOrder.setOrder_id(jObject.getInt("order_id"));
                        marketOrder.setPrice(jObject.getJsonNumber("price").doubleValue());
                        marketOrder.setRange(jObject.getString("range"));
                        marketOrder.setSystem_id(jObject.getInt("system_id"));
                        marketOrder.setType_id(jObject.getInt("type_id"));
                        marketOrder.setVolume_remain(jObject.getInt("volume_remain"));
                        marketOrder.setVolume_total(jObject.getInt("volume_total"));
                        marketOrder.setTimeStamp(new Timestamp(System.currentTimeMillis()));

                        marketOrderList.add(marketOrder);

                        System.out.println("Market Order Type ID: " + marketOrder.getType_id() + "\n Page Number: " + page_number);

                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            } else {
                break;
            }
        }
        insertMarketOrders(marketOrderList);
    }

    public static void insertMarketOrders(List<MarketOrder> marketOrderList) {
        Connection con = DatabaseConnection.connection();
        if (con == null) {
            result = "connection failure";
            return;
        }
        
        try {
            StringBuilder sqlSB = new StringBuilder();
            sqlSB.append("INSERT INTO marketorders (duration, is_buy_order, issued, location_id, min_volume, order_id, price, `range`, system_id, type_id, volume_remain, volume_total, time_fetched) VALUES");

            for(int i = 0; i < marketOrderList.size(); i++)
            {
                sqlSB.append(" (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?),");
            }
            
            sqlSB.setLength(sqlSB.length() - 1);
            
            PreparedStatement ps = null;
            ps = con.prepareStatement(sqlSB.toString());
            
            int valueCounter = 1;

            for (int i = 0; i < marketOrderList.size(); i++) {
                
                ps.setInt(valueCounter++, marketOrderList.get(i).getDuration());
                ps.setBoolean(valueCounter++, marketOrderList.get(i).isIs_buy_order());
                ps.setString(valueCounter++, marketOrderList.get(i).getIssued());
                ps.setInt(valueCounter++, marketOrderList.get(i).getLocation_id());
                ps.setInt(valueCounter++, marketOrderList.get(i).getMin_volume());
                ps.setInt(valueCounter++, marketOrderList.get(i).getOrder_id());
                ps.setDouble(valueCounter++, marketOrderList.get(i).getPrice());
                ps.setString(valueCounter++, marketOrderList.get(i).getRange());
                ps.setInt(valueCounter++, marketOrderList.get(i).getSystem_id());
                ps.setInt(valueCounter++, marketOrderList.get(i).getType_id());
                ps.setInt(valueCounter++, marketOrderList.get(i).getVolume_remain());
                ps.setInt(valueCounter++, marketOrderList.get(i).getVolume_total());
                ps.setTimestamp(valueCounter++, marketOrderList.get(i).getTimeStamp());
            }
            
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println(ex);
        }
    }

    public static void truncateTheDatabase() {
        Connection con = DatabaseConnection.connection();
        if (con == null) {
            result = "connection failure";
            return;
        }

        PreparedStatement ps = null;
        String sql = "TRUNCATE TABLE marketorders";

        try {
            ps = con.prepareStatement(sql);
            ps.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                DatabaseConnection.closeDatabaseConnection(con);
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            truncateTheDatabase();
            jsonFromUrl();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }
}
