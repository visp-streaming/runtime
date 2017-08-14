package ac.at.tuwien.infosys.visp.runtime.ui.fragements;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Configuration;

@Configuration
public class Navigation {

    public List<NavEntry> getNavEntries() {
        return Arrays.asList(
                new NavEntry("/", "/about", "About"),
                new NavEntry("/topology", "/topology", "Topology Configuration"),
                new NavEntry("/pooledvms", "/pooledvms", "Resource Pools"),
                new NavEntry("/console", "/console", "Console"),
                new NavEntry("/configuration", "/configuration", "Configuration")
                );
    }

    private static class NavEntry {
        private final String url, viewName, userFriendlyName;

        NavEntry(String url, String viewName, String userFriendlyName) {
            this.url = url;
            this.viewName = viewName;
            this.userFriendlyName = userFriendlyName;
        }

        public String getUrl() {
            return url;
        }

        public String getViewName() {
            return viewName;
        }

        public String getUserFriendlyName() {
            return userFriendlyName;
        }
    }
}
