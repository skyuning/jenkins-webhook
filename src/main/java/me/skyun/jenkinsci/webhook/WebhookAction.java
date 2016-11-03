package me.skyun.jenkinsci.webhook;

import hudson.model.Action;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by linyun on 16/11/2.
 */
public class WebhookAction implements Action {

    private static final Logger LOGGER = Logger.getLogger(WebhookAction.class.getName());

    @RequirePOST
    public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        String body = readBody(request);
        LOGGER.log(Level.INFO, "body: " + body);
        response.getWriter().print("Call webhook success!\n"
                + "Method: " + request.getMethod() + "\n"
                + "body: " + body);
    }

    @Override
    public String getIconFileName() {
        return "clock.gif";
    }

    @Override
    public String getDisplayName() {
        return "Webhook";
    }

    @Override
    public String getUrlName() {
        return "webhook";
    }

    private String readBody(StaplerRequest request) {
        try {
            return IOUtils.toString(request.getInputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }
}
