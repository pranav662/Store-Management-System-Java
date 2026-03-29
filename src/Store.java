import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

/**
 * GroceryPro — Store Management System
 * Single-file Java web application.
 */
public class Store {

    static final String DB  = "jdbc:sqlite:store.db";
    static final int    PORT = 8080;

    // ══════════════════════════════════════════════════════════════════
    // OOP — Abstract base class (abstraction + encapsulation)
    // ══════════════════════════════════════════════════════════════════
    static abstract class Product {
        private int id; private String name; private double price;
        public static final String CURRENCY = "INR";        // static + final

        Product()                        { this(0, "Unknown", 0); } // overloaded constructor
        Product(int id, String n, double p) { this.id = id; name = n; price = p; }

        int    getId()         { return id;    } void setId(int i)          { id    = i; }
        String getName()       { return name;  } void setName(String n)     { name  = n; }
        double getPrice()      { return price; } void setPrice(double p)    { price = p; }

        abstract String getType(); // polymorphism
    }

    // Inheritance — GroceryProduct extends Product
    static class GroceryProduct extends Product {
        String category, unit; int stock;

        GroceryProduct() { super(); category = "General"; unit = "pcs"; }
        GroceryProduct(int id, String name, double price, String cat, String unit, int stock) {
            super(id, name, price);                         // this() / super()
            this.category = cat; this.unit = unit; this.stock = stock;
        }
        @Override String getType() { return "Grocery"; }   // method overriding
    }

    static class Customer {
        int id; String name, phone, email, address, createdAt;
        Customer() {}
        Customer(int id, String n, String ph, String em, String addr) {
            this.id = id; name = n; phone = ph; email = em; address = addr;
        }
    }

    static class BillItem {
        int productId, quantity; String productName; double unitPrice, subtotal;
        BillItem() {}
        BillItem(int pid, String pname, int qty, double price) {
            productId = pid; productName = pname; quantity = qty;
            unitPrice = price; subtotal = qty * price;
        }
    }

    static class Bill {
        int id, customerId; String customerName, paymentMethod = "Cash", billDate;
        double totalAmount, discount; List<BillItem> items = new ArrayList<>();
    }

    // ══════════════════════════════════════════════════════════════════
    // Custom Exception
    // ══════════════════════════════════════════════════════════════════
    static class StoreException extends Exception {
        StoreException(String msg)               { super(msg); }
        StoreException(String msg, Throwable t)  { super(msg, t); }
    }

    // Interface for data access contract
    interface Dao<T> {
        void add(T item) throws StoreException;
        void delete(int id) throws StoreException;
    }

    // ══════════════════════════════════════════════════════════════════
    // JDBC — All database operations
    // ══════════════════════════════════════════════════════════════════
    static class Database {

        static void init() throws StoreException {
            // try-with-resources exception handling
            try (Connection c = conn(); Statement s = c.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,category TEXT,price REAL,stock INTEGER,unit TEXT)");
                s.execute("CREATE TABLE IF NOT EXISTS customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,phone TEXT,email TEXT,address TEXT,created_at TEXT DEFAULT(datetime('now','localtime')))");
                s.execute("CREATE TABLE IF NOT EXISTS bills(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER,customer_name TEXT,total_amount REAL,discount REAL DEFAULT 0,payment_method TEXT,bill_date TEXT DEFAULT(datetime('now','localtime')))");
                s.execute("CREATE TABLE IF NOT EXISTS bill_items(id INTEGER PRIMARY KEY AUTOINCREMENT,bill_id INTEGER,product_id INTEGER,product_name TEXT,quantity INTEGER,unit_price REAL,subtotal REAL)");
                try (ResultSet r = s.executeQuery("SELECT count(*) FROM products")) {
                    if (r.next() && r.getInt(1) == 0) seedProducts(c);
                }
                try (ResultSet r = s.executeQuery("SELECT count(*) FROM customers")) {
                    if (r.next() && r.getInt(1) == 0) seedCustomers(c);
                }
            } catch (SQLException e) { throw new StoreException("DB init failed: " + e.getMessage(), e); }
        }

        private static void seedProducts(Connection c) throws SQLException {
            String sql = "INSERT INTO products(name,category,price,stock,unit)VALUES(?,?,?,?,?)";
            Object[][] data = {
                {"Rice", "Grains", 45.0, 100, "kg"}, {"Wheat Flour", "Grains", 35.0, 80, "kg"},
                {"Sugar", "Sweeteners", 42.0, 60, "kg"}, {"Cooking Oil", "Oils", 120.0, 40, "liter"},
                {"Salt", "Spices", 15.0, 50, "kg"}, {"Milk", "Dairy", 28.0, 30, "liter"},
                {"Tomatoes", "Vegetables", 30.0, 25, "kg"}, {"Onions", "Vegetables", 20.0, 40, "kg"},
                {"Lentils", "Legumes", 80.0, 50, "kg"}, {"Tea Powder", "Beverages", 180.0, 20, "pcs"}
            };
            for (Object[] row : data) {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1,(String)row[0]); ps.setString(2,(String)row[1]);
                    ps.setDouble(3,(Double)row[2]);  ps.setInt(4,(Integer)row[3]); ps.setString(5,(String)row[4]);
                    ps.executeUpdate();
                }
            }
        }

        private static void seedCustomers(Connection c) throws SQLException {
            String sql = "INSERT INTO customers(name,phone,email,address)VALUES(?,?,?,?)";
            Object[][] data = {
                {"Rahul Sharma", "9876543210", "rahul@email.com", "12 MG Road, Pune"},
                {"Priya Patel",  "9123456789", "priya@email.com", "45 Park St, Mumbai"},
                {"Amit Kumar",   "8765432109", "amit@email.com",  "7 Lake View, Delhi"}
            };
            for (Object[] row : data) {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1,(String)row[0]); ps.setString(2,(String)row[1]);
                    ps.setString(3,(String)row[2]); ps.setString(4,(String)row[3]);
                    ps.executeUpdate();
                }
            }
        }

        // ── Products ─────────────────────────────────────────────────
        static List<GroceryProduct> getProducts() throws StoreException {
            List<GroceryProduct> list = new ArrayList<>();
            try (Connection c = conn(); Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT * FROM products ORDER BY name")) {
                while (rs.next()) {
                    list.add(new GroceryProduct(rs.getInt("id"), rs.getString("name"),
                        rs.getDouble("price"), rs.getString("category"), rs.getString("unit"), rs.getInt("stock")));
                }
            } catch (SQLException e) { throw new StoreException("Fetch products: " + e.getMessage(), e); }
            return list;
        }

        static void addProduct(GroceryProduct p) throws StoreException {
            try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO products(name,category,price,stock,unit)VALUES(?,?,?,?,?)")) {
                ps.setString(1,p.getName()); ps.setString(2,p.category); ps.setDouble(3,p.getPrice());
                ps.setInt(4,p.stock);        ps.setString(5,p.unit);     ps.executeUpdate();
            } catch (SQLException e) { throw new StoreException("Add product: " + e.getMessage(), e); }
        }

        static void updateStock(int id, int qty) throws StoreException {
            try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("UPDATE products SET stock=? WHERE id=?")) {
                ps.setInt(1,qty); ps.setInt(2,id); ps.executeUpdate();
            } catch (SQLException e) { throw new StoreException("Update stock", e); }
        }

        static void updateProduct(GroceryProduct p) throws StoreException {
            try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                    "UPDATE products SET name=?, category=?, price=?, unit=? WHERE id=?")) {
                ps.setString(1,p.getName()); ps.setString(2,p.category); ps.setDouble(3,p.getPrice());
                ps.setString(4,p.unit);       ps.setInt(5,p.getId());    ps.executeUpdate();
            } catch (SQLException e) { throw new StoreException("Update product: " + e.getMessage(), e); }
        }

        static void deleteProduct(int id) throws StoreException {
            try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id=?")) {
                ps.setInt(1,id); ps.executeUpdate();
            } catch (SQLException e) { throw new StoreException("Delete product", e); }
        }

        // ── Customers ─────────────────────────────────────────────────
        static List<Customer> getCustomers() throws StoreException {
            List<Customer> list = new ArrayList<>();
            try (Connection c = conn(); Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT * FROM customers ORDER BY name")) {
                while (rs.next()) {
                    Customer cu = new Customer(rs.getInt("id"), rs.getString("name"),
                        rs.getString("phone"), rs.getString("email"), rs.getString("address"));
                    cu.createdAt = rs.getString("created_at"); list.add(cu);
                }
            } catch (SQLException e) { throw new StoreException("Fetch customers", e); }
            return list;
        }

        static void addCustomer(Customer cu) throws StoreException {
            try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO customers(name,phone,email,address)VALUES(?,?,?,?)")) {
                ps.setString(1,cu.name); ps.setString(2,cu.phone);
                ps.setString(3,cu.email); ps.setString(4,cu.address); ps.executeUpdate();
            } catch (SQLException e) { throw new StoreException("Add customer", e); }
        }

        static void deleteCustomer(int id) throws StoreException {
            try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("DELETE FROM customers WHERE id=?")) {
                ps.setInt(1,id); ps.executeUpdate();
            } catch (SQLException e) { throw new StoreException("Delete customer", e); }
        }

        // ── Bills (with JDBC transaction) ─────────────────────────────
        static int createBill(Bill bill) throws StoreException {
            Connection c = null;
            try {
                c = conn(); c.setAutoCommit(false);
                PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO bills(customer_id,customer_name,total_amount,discount,payment_method)VALUES(?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1,bill.customerId); ps.setString(2,bill.customerName);
                ps.setDouble(3,bill.totalAmount); ps.setDouble(4,bill.discount);
                ps.setString(5,bill.paymentMethod); ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                int billId = keys.next() ? keys.getInt(1) : -1;
                for (BillItem item : bill.items) {
                    PreparedStatement ip = c.prepareStatement(
                        "INSERT INTO bill_items(bill_id,product_id,product_name,quantity,unit_price,subtotal)VALUES(?,?,?,?,?,?)");
                    ip.setInt(1,billId); ip.setInt(2,item.productId); ip.setString(3,item.productName);
                    ip.setInt(4,item.quantity); ip.setDouble(5,item.unitPrice); ip.setDouble(6,item.subtotal);
                    ip.executeUpdate();
                    PreparedStatement sp = c.prepareStatement("UPDATE products SET stock=stock-? WHERE id=?");
                    sp.setInt(1,item.quantity); sp.setInt(2,item.productId); sp.executeUpdate();
                }
                c.commit(); return billId;
            } catch (SQLException e) {
                if (c != null) try { c.rollback(); } catch (Exception ignored) {}
                throw new StoreException("Create bill: " + e.getMessage(), e);
            } finally {
                if (c != null) try { c.setAutoCommit(true); c.close(); } catch (Exception ignored) {}
            }
        }

        static List<Bill> getBills() throws StoreException {
            List<Bill> list = new ArrayList<>();
            try (Connection c = conn(); Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT * FROM bills ORDER BY id DESC")) {
                while (rs.next()) list.add(mapBill(rs));
            } catch (SQLException e) { throw new StoreException("Fetch bills", e); }
            return list;
        }

        static Bill getBillById(int id) throws StoreException {
            try (Connection c = conn()) {
                PreparedStatement ps = c.prepareStatement("SELECT * FROM bills WHERE id=?");
                ps.setInt(1,id); ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                Bill bill = mapBill(rs);
                PreparedStatement ip = c.prepareStatement("SELECT * FROM bill_items WHERE bill_id=?");
                ip.setInt(1,id); ResultSet ir = ip.executeQuery();
                while (ir.next()) {
                    BillItem item = new BillItem();
                    item.productId   = ir.getInt("product_id");    item.productName = ir.getString("product_name");
                    item.quantity    = ir.getInt("quantity");       item.unitPrice   = ir.getDouble("unit_price");
                    item.subtotal    = ir.getDouble("subtotal");    bill.items.add(item);
                }
                return bill;
            } catch (SQLException e) { throw new StoreException("Get bill", e); }
        }

        static List<Bill> getBillsByCustomer(int cid) throws StoreException {
            List<Bill> list = new ArrayList<>();
            try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM bills WHERE customer_id=? ORDER BY id DESC")) {
                ps.setInt(1,cid); ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapBill(rs));
            } catch (SQLException e) { throw new StoreException("Customer bills", e); }
            return list;
        }

        static String dashboard() throws StoreException {
            try (Connection c = conn()) {
                int tp, tc, ls; double rev;
                try (Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT count(*) FROM products")) { tp=r.next()?r.getInt(1):0; }
                try (Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT count(*) FROM customers")) { tc=r.next()?r.getInt(1):0; }
                try (Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT count(*) FROM products WHERE stock<=10")) { ls=r.next()?r.getInt(1):0; }
                try (Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT COALESCE(SUM(total_amount),0) FROM bills WHERE date(bill_date)=date('now','localtime')")) { rev=r.next()?r.getDouble(1):0; }
                StringBuilder recent = new StringBuilder("[");
                try (Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM bills ORDER BY id DESC LIMIT 5")) {
                    boolean first = true;
                    while (r.next()) {
                        if (!first) recent.append(","); first = false;
                        recent.append("{\"id\":").append(r.getInt("id"))
                              .append(",\"customerName\":\"").append(esc(r.getString("customer_name"))).append("\"")
                              .append(",\"totalAmount\":").append(r.getDouble("total_amount"))
                              .append(",\"paymentMethod\":\"").append(esc(r.getString("payment_method"))).append("\"")
                              .append(",\"billDate\":\"").append(esc(r.getString("bill_date"))).append("\"}");
                    }
                }
                recent.append("]");
                return "{\"totalProducts\":"+tp+",\"totalCustomers\":"+tc+",\"lowStockCount\":"+ls
                      +",\"todayRevenue\":"+String.format("%.2f",rev)+",\"recentBills\":"+recent+"}";
            } catch (SQLException e) { throw new StoreException("Dashboard", e); }
        }

        private static Bill mapBill(ResultSet rs) throws SQLException {
            Bill b = new Bill();
            b.id = rs.getInt("id"); b.customerId = rs.getInt("customer_id");
            b.customerName = rs.getString("customer_name"); b.totalAmount = rs.getDouble("total_amount");
            b.discount = rs.getDouble("discount"); b.paymentMethod = rs.getString("payment_method");
            b.billDate = rs.getString("bill_date"); return b;
        }

        static String esc(String s) {
            if (s == null) return "";
            return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","");
        }

        private static Connection conn() throws SQLException { return DriverManager.getConnection(DB); }
    }

    // ══════════════════════════════════════════════════════════════════
    // Networking — HTTP Server (ServerSocket)
    // ══════════════════════════════════════════════════════════════════
    static class HttpServer implements Runnable {
        private ServerSocket ss; private boolean running; final ControlPanel panel;
        HttpServer(ControlPanel p) { panel = p; }

        public void run() {
            running = true;
            try {
                ss = new ServerSocket(PORT);
                panel.log("Server running → http://localhost:" + PORT);
                while (running) {
                    Socket client = ss.accept();
                    new Thread(new RequestHandler(client, this)).start(); // new thread per request
                }
            } catch (IOException e) { if (running) panel.log("Server error: " + e.getMessage()); }
        }

        void stop() { running = false; try { if (ss != null) ss.close(); } catch (IOException e) {} }
    }

    // ══════════════════════════════════════════════════════════════════
    // Multithreading — Runnable per HTTP request
    // ══════════════════════════════════════════════════════════════════
    static class RequestHandler implements Runnable {
        private final Socket socket; private final HttpServer server;
        RequestHandler(Socket s, HttpServer srv) { socket = s; server = srv; }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter pw = new PrintWriter(socket.getOutputStream(), true)) {

                String line = in.readLine();
                if (line == null || line.isEmpty()) return;
                String[] parts = line.split(" ");
                if (parts.length < 2) return;
                String method = parts[0];
                String rawPath = parts[1];
                String path = rawPath.contains("?") ? rawPath.substring(0, rawPath.indexOf('?')) : rawPath;

                // HashMap for HTTP headers
                Map<String, String> headers = new HashMap<>();
                int contentLength = 0;
                String h;
                while ((h = in.readLine()) != null && !h.isEmpty()) {
                    int ci = h.indexOf(": ");
                    if (ci > 0) {
                        String k = h.substring(0, ci), v = h.substring(ci + 2);
                        headers.put(k, v);
                        if (k.equalsIgnoreCase("Content-Length")) contentLength = Integer.parseInt(v.trim());
                    }
                }

                // StringBuilder for body reading
                String body = "";
                if (contentLength > 0) {
                    char[] buf = new char[contentLength]; int n = 0;
                    while (n < contentLength) { int r = in.read(buf, n, contentLength - n); if (r < 0) break; n += r; }
                    body = new String(buf, 0, n);
                }

                server.panel.log(method + " " + path);
                if (path.startsWith("/api/")) handleApi(method, path, body, pw);
                else serveHtml(pw);

            } catch (Exception e) { server.panel.log("Handler error: " + e.getMessage()); }
            finally { try { socket.close(); } catch (Exception ignored) {} }
        }

        // ── API Router ────────────────────────────────────────────────
        void handleApi(String method, String path, String body, PrintWriter pw) {
            String resp;
            try {
                // switch-like if/else routing
                if      (path.equals("/api/products")           && method.equals("GET"))    resp = productsJson();
                else if (path.equals("/api/products")           && method.equals("POST"))   resp = addProductHandler(body);
                else if (path.matches("/api/products/\\d+/stock") && method.equals("POST")) { Database.updateStock(seg(path,3),(int)num(body,"stockQty")); resp=ok(); }
                else if (path.matches("/api/products/\\d+")     && method.equals("PUT"))    resp = updateProductHandler(seg(path,3), body);
                else if (path.matches("/api/products/\\d+")     && method.equals("DELETE")) { Database.deleteProduct(seg(path,3)); resp=ok(); }
                else if (path.equals("/api/customers")          && method.equals("GET"))    resp = customersJson();
                else if (path.equals("/api/customers")          && method.equals("POST"))   resp = addCustomerHandler(body);
                else if (path.matches("/api/customers/\\d+")    && method.equals("DELETE")) { Database.deleteCustomer(seg(path,3)); resp=ok(); }
                else if (path.matches("/api/customers/\\d+/bills") && method.equals("GET")) resp = billsJson(Database.getBillsByCustomer(seg(path,3)));
                else if (path.equals("/api/bills")              && method.equals("GET"))    resp = billsJson(Database.getBills());
                else if (path.equals("/api/bills")              && method.equals("POST"))   resp = createBillHandler(body);
                else if (path.matches("/api/bills/\\d+")        && method.equals("GET"))    { Bill b=Database.getBillById(seg(path,3)); resp=b!=null?billDetailJson(b):"{\"error\":\"not found\"}"; }
                else if (path.equals("/api/dashboard")          && method.equals("GET"))    resp = Database.dashboard();
                else resp = "{\"error\":\"Not found\"}";
            } catch (Exception e) { resp = "{\"error\":\"" + Database.esc(e.getMessage()) + "\"}"; }
            sendJson(pw, resp);
        }

        String productsJson() throws StoreException {
            List<GroceryProduct> list = Database.getProducts();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                GroceryProduct p = list.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"id\":").append(p.getId())
                  .append(",\"name\":\"").append(Database.esc(p.getName())).append("\"")
                  .append(",\"category\":\"").append(Database.esc(p.category)).append("\"")
                  .append(",\"price\":").append(p.getPrice())
                  .append(",\"stockQty\":").append(p.stock)
                  .append(",\"unit\":\"").append(Database.esc(p.unit)).append("\"}");
            }
            return sb.append("]").toString();
        }

        String addProductHandler(String body) throws StoreException {
            GroceryProduct p = new GroceryProduct();
            p.setName(str(body,"name")); p.category = str(body,"category");
            p.setPrice(num(body,"price")); p.stock = (int)num(body,"stockQty"); p.unit = str(body,"unit");
            Database.addProduct(p); return ok();
        }

        String updateProductHandler(int id, String body) throws StoreException {
            GroceryProduct p = new GroceryProduct();
            p.setId(id); p.setName(str(body,"name")); p.category = str(body,"category");
            p.setPrice(num(body,"price")); p.unit = str(body,"unit");
            Database.updateProduct(p); return ok();
        }

        String customersJson() throws StoreException {
            List<Customer> list = Database.getCustomers();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                Customer c = list.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"id\":").append(c.id)
                  .append(",\"name\":\"").append(Database.esc(c.name)).append("\"")
                  .append(",\"phone\":\"").append(Database.esc(c.phone)).append("\"")
                  .append(",\"email\":\"").append(Database.esc(c.email)).append("\"")
                  .append(",\"address\":\"").append(Database.esc(c.address)).append("\"")
                  .append(",\"createdAt\":\"").append(Database.esc(c.createdAt)).append("\"}");
            }
            return sb.append("]").toString();
        }

        String addCustomerHandler(String body) throws StoreException {
            Customer cu = new Customer();
            cu.name = str(body,"name"); cu.phone = str(body,"phone");
            cu.email = str(body,"email"); cu.address = str(body,"address");
            Database.addCustomer(cu); return ok();
        }

        String billsJson(List<Bill> bills) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < bills.size(); i++) { if (i>0) sb.append(","); sb.append(billHeader(bills.get(i))); }
            return sb.append("]").toString();
        }

        String billHeader(Bill b) {
            return "{\"id\":"+b.id+",\"customerId\":"+b.customerId
                  +",\"customerName\":\""+Database.esc(b.customerName)+"\""
                  +",\"totalAmount\":"+b.totalAmount+",\"discount\":"+b.discount
                  +",\"paymentMethod\":\""+Database.esc(b.paymentMethod)+"\""
                  +",\"billDate\":\""+Database.esc(b.billDate)+"\"}";
        }

        String billDetailJson(Bill b) {
            String head = billHeader(b);
            StringBuilder sb = new StringBuilder(head.substring(0, head.length()-1)); // strip closing }
            sb.append(",\"items\":[");
            for (int i = 0; i < b.items.size(); i++) {
                BillItem it = b.items.get(i);
                if (i>0) sb.append(",");
                sb.append("{\"productName\":\"").append(Database.esc(it.productName)).append("\"")
                  .append(",\"quantity\":").append(it.quantity)
                  .append(",\"unitPrice\":").append(it.unitPrice)
                  .append(",\"subtotal\":").append(it.subtotal).append("}");
            }
            return sb.append("]}").toString();
        }

        String createBillHandler(String body) throws StoreException {
            Bill bill = new Bill();
            bill.customerId   = (int)num(body,"customerId");
            bill.customerName = str(body,"customerName");
            bill.totalAmount  = num(body,"totalAmount");
            bill.discount     = num(body,"discount");
            bill.paymentMethod= str(body,"paymentMethod");
            for (String obj : jsonArr(body,"items")) {
                BillItem it = new BillItem();
                it.productId   = (int)num(obj,"productId");   it.productName = str(obj,"productName");
                it.quantity    = (int)num(obj,"quantity");     it.unitPrice   = num(obj,"unitPrice");
                it.subtotal    = num(obj,"subtotal");          bill.items.add(it);
            }
            int id = Database.createBill(bill);
            return "{\"success\":true,\"billId\":"+id+"}";
        }

        // ── JSON / HTTP helpers ────────────────────────────────────────
        String str(String json, String key) {
            String pat = "\"" + key + "\":\"";
            int s = json.indexOf(pat); if (s < 0) return "";
            s += pat.length(); int e = s;
            while (e < json.length()) { if (json.charAt(e)=='"' && (e==0||json.charAt(e-1)!='\\')) break; e++; }
            return json.substring(s, e);
        }

        double num(String json, String key) {
            String pat = "\"" + key + "\":";
            int s = json.indexOf(pat); if (s < 0) return 0;
            s += pat.length();
            while (s < json.length() && json.charAt(s)==' ') s++;
            int e = s; while (e < json.length() && "0123456789.-".indexOf(json.charAt(e))>=0) e++;
            try { return Double.parseDouble(json.substring(s,e)); } catch (Exception ex) { return 0; }
        }

        List<String> jsonArr(String json, String key) {
            List<String> res = new ArrayList<>();
            String pat = "\"" + key + "\":[";
            int s = json.indexOf(pat); if (s < 0) return res;
            s += pat.length() - 1; int depth=0, end=s;
            for (int i=s; i<json.length(); i++) { char c=json.charAt(i); if(c=='[') depth++; else if(c==']'){depth--;if(depth==0){end=i;break;}} }
            String content = json.substring(s+1, end).trim(); if (content.isEmpty()) return res;
            int bd=0, start=0;
            for (int j=0; j<content.length(); j++) {
                char c = content.charAt(j);
                if (c=='{') { if(bd==0) start=j; bd++; } else if (c=='}') { bd--; if(bd==0) res.add(content.substring(start,j+1)); }
            }
            return res;
        }

        int seg(String path, int idx) { return Integer.parseInt(path.split("/")[idx]); }
        String ok() { return "{\"success\":true}"; }

        void sendJson(PrintWriter pw, String json) {
            pw.print("HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\n"
                    +"Access-Control-Allow-Origin: *\r\nContent-Length: "+json.getBytes().length+"\r\n\r\n"+json);
            pw.flush();
        }

        // Scanner to read HTML file, StringBuilder to build response
        void serveHtml(PrintWriter pw) {
            File f = new File("static_web/index.html");
            if (!f.exists()) { sendJson(pw, "{\"error\":\"index.html not found\"}"); return; }
            StringBuilder sb = new StringBuilder();
            try (Scanner sc = new Scanner(f)) { while (sc.hasNextLine()) sb.append(sc.nextLine()).append("\n"); }
            catch (FileNotFoundException e) { sendJson(pw, "{\"error\":\"Read error\"}"); return; }
            String html = sb.toString();
            pw.print("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n"
                    +"Content-Length: "+html.getBytes().length+"\r\n\r\n"+html);
            pw.flush();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // AWT/Swing GUI — Event-driven Control Panel
    // ══════════════════════════════════════════════════════════════════
    static class ControlPanel extends JFrame implements ActionListener {
        private final JButton startBtn = new JButton("▶ Start Server");
        private final JButton stopBtn  = new JButton("■ Stop Server");
        private final JTextArea logArea = new JTextArea();
        private HttpServer server;
        private Thread serverThread;

        ControlPanel() {
            super("GroceryPro — Store Management System");
            server = new HttpServer(this);
            setSize(520, 380);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            // AWT Panel with buttons (AWT component/container)
            JPanel topPanel = new JPanel(new FlowLayout());
            stopBtn.setEnabled(false);
            startBtn.addActionListener(this);   // Event listener
            stopBtn.addActionListener(this);
            startBtn.setBackground(new Color(16,185,129)); startBtn.setForeground(Color.WHITE);
            stopBtn.setBackground(new Color(239,68,68));   stopBtn.setForeground(Color.WHITE);
            topPanel.add(startBtn); topPanel.add(stopBtn);
            topPanel.add(new JLabel("    Open: http://localhost:" + PORT));
            add(topPanel, BorderLayout.NORTH);

            // JTextArea for log display (Swing component)
            logArea.setEditable(false);
            logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            logArea.setBackground(new Color(17,24,39)); logArea.setForeground(new Color(156,163,175));
            add(new JScrollPane(logArea), BorderLayout.CENTER);

            JLabel footer = new JLabel("  GroceryPro | Java ServerSocket + JDBC + Swing", SwingConstants.LEFT);
            footer.setFont(new Font("SansSerif", Font.ITALIC, 11));
            add(footer, BorderLayout.SOUTH);

            log("Ready. Click 'Start Server', then open http://localhost:" + PORT + " in your browser.");
            setLocationRelativeTo(null);
        }

        // Event handling
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == startBtn) {
                serverThread = new Thread(server);
                serverThread.start();
                startBtn.setEnabled(false); stopBtn.setEnabled(true);
            } else {
                server.stop();
                startBtn.setEnabled(true); stopBtn.setEnabled(false);
                log("Server stopped.");
                // Allow restart
                server = new HttpServer(this);
            }
        }

        // Thread-safe logging from server threads to Swing
        void log(String msg) {
            SwingUtilities.invokeLater(() -> {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Entry point
    // ══════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        // Exception handling for DB init
        try {
            Database.init();
            System.out.println("Database initialized.");
        } catch (StoreException e) {
            System.err.println("Fatal: " + e.getMessage()); return;
        }
        // Launch Swing GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new ControlPanel().setVisible(true));
    }
}
