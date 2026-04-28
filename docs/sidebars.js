const sidebars = {
  docs: [
    "intro",
    {
      type: "category",
      label: "Installation",
      link: {
        type: "doc",
        id: "installation/index"
      },
      items: [
        "installation/normal/index",
        "installation/advanced/index"
      ]
    },
    {
      type: "category",
      label: "Providers",
      link: {
        type: "doc",
        id: "providers/index"
      },
      items: [
        {
          type: "category",
          label: "Official Providers",
          link: {
            type: "doc",
            id: "providers/official/index"
          },
          items: [
            "providers/official/premium/index",
            "providers/official/cracked/index"
          ]
        },
        "providers/community/index"
      ]
    },
    "configuration/index",
    "developer/index"
  ]
};

module.exports = sidebars;
