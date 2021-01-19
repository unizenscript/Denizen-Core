package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;

import java.util.ArrayList;
import java.util.List;

public class WhileCommand extends BracedCommand {

    public WhileCommand() {
        setName("while");
        setSyntax("while [stop/next/[<value>] (!)(<operator> <value>) (&&/|| ...)] [<commands>]");
        setRequiredArguments(1, -1);
        setParseArgs(false);
        isProcedural = true;
    }

    // <--[command]
    // @Name While
    // @Syntax while [stop/next/[<value>] (!)(<operator> <value>) (&&/|| ...)] [<commands>]
    // @Required 1
    // @Maximum -1
    // @Short Runs a series of braced commands until the tag returns false.
    // @Group queue
    // @Guide https://guide.denizenscript.com/guides/basics/loops.html
    //
    // @Description
    // Runs a series of braced commands until the tag returns false.
    // To end a while loop, use the 'stop' argument.
    // To jump to the next entry in the loop, use the 'next' argument.
    //
    // @Tags
    // <[loop_index]> to get the number of loops so far.
    //
    // @Usage
    // Use to loop until a player sneaks, or the player goes offline. (Note: generally use 'waituntil' for this instead)
    // - while !<player.is_sneaking> && <player.is_online>:
    //     - narrate "Waiting for you to sneak..."
    //     - wait 1s
    //
    // -->

    private class WhileData {
        public int index;
        public List<String> value;
        public long LastChecked;
        int instaTicks;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        List<String> comparisons = new ArrayList<>();
        if (scriptEntry.getArguments().size() == 1) {
            String arg = scriptEntry.getArguments().get(0);
            if (CoreUtilities.equalsIgnoreCase(arg, "stop")) {
                scriptEntry.addObject("stop", new ElementTag(true));
            }
            else if (CoreUtilities.equalsIgnoreCase(arg, "next")) {
                scriptEntry.addObject("next", new ElementTag(true));
            }
            else if (arg.equals("\0CALLBACK")) {
                scriptEntry.addObject("callback", new ElementTag(true));
            }
        }
        for (String arg : scriptEntry.getArguments()) {
            if (arg.equals("{")) {
                break;
            }
            comparisons.add(arg);
        }
        if (comparisons.isEmpty() && !scriptEntry.hasObject("stop") && !scriptEntry.hasObject("next") && !scriptEntry.hasObject("callback")) {
            throw new InvalidArgumentsException("Must specify a comparison value or 'stop' or 'next'!");
        }
        scriptEntry.addObject("braces", getBracedCommands(scriptEntry));
        scriptEntry.addObject("comparisons", comparisons);

    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag stop = scriptEntry.getElement("stop");
        ElementTag next = scriptEntry.getElement("next");
        ElementTag callback = scriptEntry.getElement("callback");
        if (stop != null && stop.asBoolean()) {
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), stop.debug());
            }
            boolean hasnext = false;
            for (int i = 0; i < scriptEntry.getResidingQueue().getQueueSize(); i++) {
                ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(i);
                List<String> args = entry.getOriginalArguments();
                if (entry.getCommandName().equals("WHILE") && args.size() > 0 && args.get(0).equals("\0CALLBACK")) {
                    hasnext = true;
                    break;
                }
            }
            if (hasnext) {
                while (scriptEntry.getResidingQueue().getQueueSize() > 0) {
                    ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(0);
                    List<String> args = entry.getOriginalArguments();
                    if (entry.getCommandName().equals("WHILE") && args.size() > 0 && args.get(0).equals("\0CALLBACK")) {
                        scriptEntry.getResidingQueue().removeEntry(0);
                        break;
                    }
                    scriptEntry.getResidingQueue().removeEntry(0);
                }
            }
            else {
                Debug.echoError(scriptEntry.getResidingQueue(), "Cannot stop while: not in one!");
            }
            return;
        }
        else if (next != null && next.asBoolean()) {
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), next.debug());
            }
            boolean hasnext = false;
            for (int i = 0; i < scriptEntry.getResidingQueue().getQueueSize(); i++) {
                ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(i);
                List<String> args = entry.getOriginalArguments();
                if (entry.getCommandName().equals("WHILE") && args.size() > 0 && args.get(0).equals("\0CALLBACK")) {
                    hasnext = true;
                    break;
                }
            }
            if (hasnext) {
                while (scriptEntry.getResidingQueue().getQueueSize() > 0) {
                    ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(0);
                    List<String> args = entry.getOriginalArguments();
                    if (entry.getCommandName().equals("WHILE") && args.size() > 0 && args.get(0).equals("\0CALLBACK")) {
                        break;
                    }
                    scriptEntry.getResidingQueue().removeEntry(0);
                }
            }
            else {
                Debug.echoError(scriptEntry.getResidingQueue(), "Cannot stop while: not in one!");
            }
            return;
        }
        else if (callback != null && callback.asBoolean()) {
            if (scriptEntry.getOwner() != null && (scriptEntry.getOwner().getCommandName().equals("WHILE") ||
                    scriptEntry.getOwner().getBracedSet() == null || scriptEntry.getOwner().getBracedSet().isEmpty() ||
                    scriptEntry.getBracedSet().get(0).value.get(scriptEntry.getBracedSet().get(0).value.size() - 1) != scriptEntry)) {
                WhileData data = (WhileData) scriptEntry.getOwner().getData();
                data.index++;
                if (System.currentTimeMillis() - data.LastChecked < 50) {
                    data.instaTicks++;
                    int max = DenizenCore.getImplementation().whileMaxLoops();
                    if (data.instaTicks > max && max != 0) {
                        return;
                    }
                }
                else {
                    data.instaTicks = 0;
                }
                data.LastChecked = System.currentTimeMillis();
                boolean run = new IfCommand.ArgComparer().compare(new ArrayList(data.value), scriptEntry);
                if (run) {
                    if (scriptEntry.dbCallShouldDebug()) {
                        Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, "While loop " + data.index);
                    }
                    scriptEntry.getResidingQueue().addDefinition("loop_index", String.valueOf(data.index));
                    List<ScriptEntry> bracedCommands = BracedCommand.getBracedCommands(scriptEntry.getOwner()).get(0).value;
                    ScriptEntry callbackEntry = scriptEntry.clone();
                    callbackEntry.copyFrom(scriptEntry);
                    callbackEntry.setOwner(scriptEntry.getOwner());
                    bracedCommands.add(callbackEntry);
                    for (int i = 0; i < bracedCommands.size(); i++) {
                        bracedCommands.get(i).setInstant(true);
                    }
                    scriptEntry.getResidingQueue().injectEntries(bracedCommands, 0);
                }
                else {
                    if (scriptEntry.dbCallShouldDebug()) {
                        Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, "While loop complete");
                    }
                }
            }
            else {
                Debug.echoError(scriptEntry.getResidingQueue(), "While CALLBACK invalid: not a real callback!");
            }
        }
        else {
            List<String> comparisons = (List<String>) scriptEntry.getObject("comparisons");
            List<BracedData> data = ((List<BracedData>) scriptEntry.getObject("braces"));
            if (data == null || data.isEmpty()) {
                Debug.echoError(scriptEntry.getResidingQueue(), "Empty braces (internal)!");
                return;
            }
            List<ScriptEntry> bracedCommandsList = data.get(0).value;

            if (bracedCommandsList == null || bracedCommandsList.isEmpty()) {
                Debug.echoError(scriptEntry.getResidingQueue(), "Empty braces!");
                return;
            }
            boolean run = new IfCommand.ArgComparer().compare(comparisons, scriptEntry);

            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), ArgumentHelper.debugObj("run_first_loop", run));
            }

            if (!run) {
                return;
            }

            WhileData datum = new WhileData();
            datum.index = 1;
            datum.value = comparisons;
            datum.LastChecked = System.currentTimeMillis();
            datum.instaTicks = 1;
            scriptEntry.setData(datum);
            ScriptEntry callbackEntry = new ScriptEntry("WHILE", new String[] {"\0CALLBACK"},
                    (scriptEntry.getScript() != null ? scriptEntry.getScript().getContainer() : null));
            callbackEntry.copyFrom(scriptEntry);
            callbackEntry.setOwner(scriptEntry);
            bracedCommandsList.add(callbackEntry);
            scriptEntry.getResidingQueue().addDefinition("loop_index", "1");
            for (int i = 0; i < bracedCommandsList.size(); i++) {
                bracedCommandsList.get(i).setInstant(true);
            }
            scriptEntry.setInstant(true);
            scriptEntry.getResidingQueue().injectEntries(bracedCommandsList, 0);
        }
    }
}
