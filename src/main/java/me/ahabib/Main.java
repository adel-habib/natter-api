package me.ahabib;

import me.ahabib.controller.SpaceController;
import org.dalesbred.Database;
import org.h2.jdbcx.JdbcConnectionPool;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EmptyStackException;

import static spark.Spark.*;

public class Main {
    public static void main(String... args) throws Exception {


        JdbcConnectionPool dataSource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter", "password");
        Database database = Database.forDataSource(dataSource);
        createTables(database);
        dataSource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter_api_user", "password");
        var spaceController = new SpaceController(database);
        exception(IllegalArgumentException.class,Main::badRequest);
        exception(EmptyStackException.class, ((e, request, response) -> {response.status(404);}) );
        before(((request, response) -> {
            if (request.requestMethod().equals("POST") &&
                    !"application/json".equals(request.contentType())) {
                halt(415, new JSONObject().put(
                        "error", "Only application/json supported"
                ).toString());
            }
        }));

        afterAfter((request, response) -> {
            response.type("application/json;charset=utf-8");
            response.header("X-Content-Type-Options", "nosniff");
            response.header("X-Frame-Options", "DENY");
            response.header("X-XSS-Protection", "0");
            response.header("Cache-Control", "no-store");
            response.header("Content-Security-Policy",
                    "default-src 'none'; frame-ancestors 'none'; sandbox");
            response.header("Server", "");
        });

        post("/spaces", spaceController::createSpace);
        after((request, response) -> {response.type("application/json");});
        internalServerError(new JSONObject().put("error", "internal server error").toString());
        notFound(new JSONObject().put("error", "not found").toString());
    }

    private static void createTables(Database database) throws Exception {
        Path path = Paths.get(Main.class.getResource("/schema.sql").toURI());
        database.update(Files.readString(path));
    }
    private static void badRequest(Exception exception, Request request, Response response) {
        response.status(400);
        response.body("{\"error\": \"" + exception.getMessage() + "\"}");
    }
}
