package clonedetector;

import indexbased.SearchManager;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;

import userInterface.VersionCheck;
import userInterface.WatchUserBehavior;
import astParser.ASTMethodParser;

public class PluginStartup implements IStartup {
    private HashMap<String, String> mapAST;
    private HashMap<String, SearchManager> mapSearchManager;
    public IWorkspace workspace;

    @Override
    public void earlyStartup() {
        mapAST = new HashMap<String, String>();
        mapSearchManager = new HashMap<String, SearchManager>();
        //
        // 1. Parsing source code
        // 2. Generates term-frequency files
        //
        ASTMethodParser ast = new ASTMethodParser();
        try {
            ast.execute(mapAST);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //
        // Generates Indexes
        //
        SearchManager searchManager = null;
        for (String key : mapAST.keySet()) {
            String path = mapAST.get(key);
            try {
                searchManager = new SearchManager(path);
                searchManager.initIndexEnv();
                searchManager.doIndex();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            mapSearchManager.put(key, searchManager);
        }

        // If one of dependencies' version cannot run this plugin,
        // an error message is shown and plugin would be terminated at this
        // point.
        if (VersionCheck.checkVersion() == false)
            return;

        //
        // UI
        //

        WatchUserBehavior watchUserBehavior = new WatchUserBehavior(ast,
                mapAST, mapSearchManager);
        watchUserBehavior.run();

    }
}
