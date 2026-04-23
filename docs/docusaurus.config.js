const fs = require("fs");
const path = require("path");

const versionsPath = path.join(__dirname, "versions.json");
const versions = fs.existsSync(versionsPath) ? require(versionsPath) : [];
const lastReleaseVersion = versions.find((version) => version.startsWith("v"));
const defaultVersion = lastReleaseVersion || versions[0];

const versionConfig = Object.fromEntries(
  versions.map((version) => [
    version,
    version === "branch-release"
      ? {
          label: "release",
          path: "release",
          badge: false
        }
      : {
          label: version,
          badge: false
        }
  ])
);

const config = {
  title: "Identica",
  tagline: "Modular next-gen Minecraft identity plugin with pluggable providers",
  favicon: "img/favicon.ico",

  url: "https://identica.whereareiam.me",
  baseUrl: "/",

  organizationName: "whereareiam",
  projectName: "Identica",

  onBrokenLinks: "throw",
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: "warn"
    }
  },

  i18n: {
    defaultLocale: "en",
    locales: ["en"]
  },

  presets: [
    [
      "classic",
      {
        docs: {
          path: "content",
          routeBasePath: "/",
          sidebarPath: require.resolve("./sidebars.js"),
          lastVersion: defaultVersion || "current",
          versions: {
            current: {
              label: "dev",
              path: defaultVersion ? "dev" : "/",
              badge: false
            },
            ...versionConfig
          },
          editUrl: "https://github.com/whereareiam/Identica/tree/dev/"
        },
        blog: false,
        theme: {
          customCss: require.resolve("./src/css/custom.css")
        }
      }
    ]
  ],

  themeConfig: {
    colorMode: {
      respectPrefersColorScheme: true
    },
    navbar: {
      title: "Identica",
      items: [
        {
          type: "docsVersionDropdown",
          position: "right"
        },
        {
          href: "https://github.com/whereareiam/Identica",
          label: "GitHub",
          position: "right"
        }
      ]
    },
    footer: {
      style: "dark",
      copyright: `Copyright © ${new Date().getFullYear()} whereareiam`
    },
    prism: {
      additionalLanguages: ["java", "kotlin", "yaml"]
    }
  }
};

module.exports = config;
