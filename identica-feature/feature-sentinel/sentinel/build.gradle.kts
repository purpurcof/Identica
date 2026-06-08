plugins {
    id("shared")
}

dependencies {
    api(projects.featureSentinelApi)
    implementation(projects.featureSentinelCommon)
}
