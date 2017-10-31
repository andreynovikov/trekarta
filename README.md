[![Build Status](https://travis-ci.org/andreynovikov/maptrek.svg?branch=master)](https://travis-ci.org/andreynovikov/maptrek)
[![GitHub release](https://img.shields.io/github/release/andreynovikov/maptrek.svg)](https://github.com/andreynovikov/maptrek/releases/latest)
[![GitHub license](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)

__MapTrek__ is designed for hiking, geocaching, off-roading, cycling, boating and all other outdoor activities. It uses offline maps so you do not need to have internet connection. You can easily import waypoints and tracks from GPX and KML data formats or create waypoints in application and share them to others. It lets you write the track of your journey, even in background, so you will never get lost and be able later see where you've been.

![](http://maptrek.mobi/images/screenshot02.png)

MapTrek is developed as a hobby for personal use but is generously shared to public on as-is basis. Developers are welcome to contribute to the project. Specifically experts in OpenGL and PostGIS are much needed.

The development is in beta stage now. Bug reports and feature proposals are welcome in [issue tracker](https://github.com/andreynovikov/maptrek/issues), for questions and general discussions [Google Groups Q&A Forum](https://groups.google.com/forum/#!forum/maptrek) should be preferred.

## How to build

1. Clone maptrek repository
2. Clone maptrek branch of vtm repository
3. Run the following graddle tasks (these will install required vtm snapshots in local Maven repository):

    vtm:vtm install
    vtm:vtm-android install
    vtm:vtm-android install

4. Clone androidcolorpicker repository
5. Run install task
6. Clone maptrek submodules

    git submodule update --init --recursive

7. Build maptrek project

