package me.skyun.jenkinsci.webhook;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by linyun on 16/11/2.
 */
@Extension
public class WebhookActionFactory extends TransientProjectActionFactory {
    @Override
    public Collection<? extends Action> createFor(AbstractProject abstractProject) {
        return Collections.singleton(new WebhookAction());
    }
}
