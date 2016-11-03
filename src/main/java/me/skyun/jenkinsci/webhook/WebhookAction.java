package me.skyun.jenkinsci.webhook;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Action;
import hudson.model.Descriptor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONTokener;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by linyun on 16/11/2.
 */
public class WebhookAction extends AbstractDescribableImpl<WebhookAction> implements Action {

    private static final Logger LOGGER = Logger.getLogger(WebhookAction.class.getName());
    private static final String CODE_REVIEW_PASS_MSG = "PreMerge测试通过，可以Merge :100:";
    private static final String URL_FORMAT = "https://git.chunyu.me/api/v3" +
            "/projects/%s" +
            "/merge_requests/%s/%s" +
            "?private_token=mLiMjFk2rg94hPrwrMgJ";

    @RequirePOST
    public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        LOGGER.log(Level.INFO, "webhook triggered");
        String body = readInputStream(request.getInputStream());
        JSONTokener tokener = new JSONTokener(body);
        JSONObject object = (JSONObject) tokener.nextValue();
        String projectId = object.getString("project_id");
        String mrId = object.getJSONObject("merge_request").getString("id");

        String baseMRUrl = String.format(Locale.getDefault(), URL_FORMAT, projectId, mrId, "%s");

        if (isPassUpvote(baseMRUrl) && isPass100(baseMRUrl)) {
            String errMsg = acceptMergeRequest(baseMRUrl);
            String responseMsg;
            if (errMsg == null) {
                responseMsg = "Accept Merge Request success";
            } else {
                responseMsg = errMsg;
            }
            LOGGER.log(Level.INFO, responseMsg);
            response.getWriter().print(responseMsg);
        } else {
            response.getWriter().print("Can not merge!\n");
        }
    }

    private boolean isPassUpvote(String baseMRUrl) throws IOException {
        URL url = new URL(String.format(Locale.getDefault(), baseMRUrl, ""));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        String body = readInputStream(connection.getInputStream());
        JSONTokener tokener = new JSONTokener(body);
        JSONObject mergeRequest = (JSONObject) tokener.nextValue();
        int upvotes = mergeRequest.optInt("upvotes");
        LOGGER.log(Level.INFO, "Upvotes: " + upvotes);
        return upvotes >= 2;
    }

    private boolean isPass100(String baseMRUrl) throws IOException {
        URL url = new URL(String.format(Locale.getDefault(), baseMRUrl, "notes"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        String body = readInputStream(connection.getInputStream());
        JSONTokener tokener = new JSONTokener(body);
        JSONArray notes = (JSONArray) tokener.nextValue();
        for (int i = 0; i < notes.size(); i++) {
            String noteBody = notes.getJSONObject(i).optString("body", "");
            if (noteBody.contains(CODE_REVIEW_PASS_MSG)) {
                LOGGER.log(Level.INFO, "Pass code analysis");
                return true;
            }
        }
        LOGGER.log(Level.INFO, "Code analysis not passed");
        return false;
    }

    private String acceptMergeRequest(String baseMRUrl) throws IOException {
        URL url = new URL(String.format(Locale.getDefault(), baseMRUrl, "merge"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.connect();
        int code = connection.getResponseCode();
        String errMsg;
        if (code == 401) {
            errMsg = "you don't have permissions to accept this merge request";
        } else if (code == 405) {
            errMsg = "merge request is already merged or closed";
        } else if (code == 406) {
            errMsg = "the merge request is not set to be merged when the build succeeds";
        } else {
            errMsg = null;
        }
        return errMsg;
    }

    @Override
    public WebHookDescriptor getDescriptor() {
        return (WebHookDescriptor) super.getDescriptor();
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

    private String readInputStream(InputStream is) {
        try {
            return IOUtils.toString(is);
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    @Extension
    public static final class WebHookDescriptor extends Descriptor<WebhookAction> {

        String mToken = "";

        public WebHookDescriptor() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "WebHookAction";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            JSONObject config = json.getJSONObject("webhook");
            mToken = config.getString("token");
            save();
            return true;
        }
    }
}
