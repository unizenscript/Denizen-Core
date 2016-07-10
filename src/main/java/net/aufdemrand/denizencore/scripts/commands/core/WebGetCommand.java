package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.scripts.commands.Holdable;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.aufdemrand.denizencore.utilities.scheduling.Schedulable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebGetCommand extends AbstractCommand implements Holdable {
    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (!scriptEntry.hasObject("url")) {
                scriptEntry.addObject("url", new Element(arg.raw_value));
            }

            else {
                arg.reportUnhandled();
            }
        }

        if (!scriptEntry.hasObject("url")) {
            throw new InvalidArgumentsException("Must have a valid URL!");
        }

        Element url = scriptEntry.getElement("url");
        if (!url.asString().startsWith("http://") && !url.asString().startsWith("https://")) {
            throw new InvalidArgumentsException("Must have a valid (HTTP/HTTPS) URL! Attempted: " + url.asString());
        }

    }


    @Override
    public void execute(final ScriptEntry scriptEntry) throws CommandExecutionException {

        if (!DenizenCore.getImplementation().allowedToWebget()) {
            dB.echoError(scriptEntry.getResidingQueue(), "WebGet disabled by config!");
            return;
        }

        final Element url = scriptEntry.getElement("url");

        dB.report(scriptEntry, getName(), url.debug());

        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                webGet(scriptEntry, url);
            }
        });
        thr.start();
    }

    public void webGet(final ScriptEntry scriptEntry, Element urlp) {

        BufferedReader in = null;
        try {
            // Open a connection to the paste server
            URL url = new URL(urlp.asString());
            HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            uc.setDoInput(true);
            uc.setDoOutput(true);
            uc.setConnectTimeout(10000);
            uc.connect();
            in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            final StringBuilder sb = new StringBuilder();
            // Probably a better way to do this bit.
            while (true) {
                try {
                    String temp = in.readLine();
                    if (temp == null) {
                        break;
                    }
                    sb.append(temp);
                }
                catch (Exception ex) {
                    break;
                }
            }
            in.close();
            DenizenCore.schedule(new Schedulable() {
                @Override
                public boolean tick(float seconds) {
                    scriptEntry.addObject("failed", new Element("false"));
                    scriptEntry.addObject("result", new Element(sb.toString()));
                    scriptEntry.setFinished(true);
                    return false;
                }
            });
        }
        catch (Exception e) {
            dB.echoError(e);
        }
        finally {
            try {
                DenizenCore.schedule(new Schedulable() {
                    @Override
                    public boolean tick(float seconds) {
                        scriptEntry.addObject("failed", new Element("true"));
                        scriptEntry.setFinished(true);
                        return false;
                    }
                });
                if (in != null) {
                    in.close();
                }
            }
            catch (Exception e) {
                dB.echoError(e);
            }
        }
    }
}
