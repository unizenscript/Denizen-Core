package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.DenizenCore;

public class ReloadScriptsScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // reload scripts
    // script reload
    //
    // @Switch had_error:true/false to only process the event if there either was or was not an error message.
    //
    // @Regex ^on ((reload scripts)|(script reload))$
    //
    // @Group Core
    //
    // @Triggers when Denizen scripts are reloaded.
    //
    // @Context
    // <context.had_error> returns an ElementTag(Boolean) whether there was an error.
    //
    // -->

    public static ReloadScriptsScriptEvent instance;

    public boolean hadError = false;

    @Override
    public ScriptEntryData getScriptEntryData() {
        return DenizenCore.getImplementation().getEmptyScriptEntryData();
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("had_error")) {
            return new ElementTag(hadError);
        }
        return super.getContext(name);
    }

    public ReloadScriptsScriptEvent() {
        instance = this;
    }

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("reload scripts") || path.eventLower.startsWith("script reload");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!path.checkSwitch("had_error", hadError ? "true" : "false")) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "ReloadScripts";
    }
}
