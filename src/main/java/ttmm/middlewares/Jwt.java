package ttmm.middlewares;


import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import ttmm.database.models.User;
import ttmm.database.repos.UserRepo;
import ttmm.utils.ConfigManager;
import ttmm.utils.Response;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public enum Jwt {

    INSTANCE;
    private JWTAuth provider = null;

    public void inti(Vertx vertx) {

        if (provider != null) return;

        provider = JWTAuth.create(vertx, new JWTAuthOptions().addPubSecKey(new PubSecKeyOptions()
            .setAlgorithm("HS256")
            .setBuffer(ConfigManager.INSTANCE.getJwtSecret())));

    }

    public String generateToken(String email, Long id) {
        try {
            return provider.generateToken(new JsonObject().put("email", email).put("id", id));
        } catch (Exception e) {
            log.error("Generating token " + e.getMessage());
            throw new RuntimeException("Error generating token");
        }
    }

    private User isValidToken(String token) {
        try {
            AtomicReference<User> userInfo = new AtomicReference<>();
            provider.authenticate(new TokenCredentials(token)).onSuccess(user -> {
                userInfo.set(UserRepo.INSTANCE.getBasicDetails(user.principal().getLong("id"), null));
            }).onFailure(error -> {
                throw new RuntimeException("Token is invalid");
            });
            return userInfo.get();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void validateToken(RoutingContext context) {
        Single.just(context).map(ctx -> {
            String token = ctx.request().getHeader("Authorization");
            if (token == null) {
                throw new RuntimeException("Token is required");
            }
            token = token.replace("Bearer ", "");

            User userData = isValidToken(token);
            if (userData == null)
                throw new RuntimeException("User not found");
            userData.setUserGroups(null);
            userData.setCategories(null);
            userData.setUserTransactions(null);
            ctx.put("user", userData);
            return ctx;
        }).subscribe(
            RoutingContext::next,
            error -> ResponseHelper.INSTANCE.writeJsonResponse(context, new Response(error.getMessage(), null, 400, false))
        ).dispose();
    }


}
