function versionSelector(list) {
  // the version we want
  newVersion = list[list.selectedIndex].value;

  // spilt the current path
  var pathArray = window.location.pathname.split( '/' );

  // so we can get the current version
  currentVersion = pathArray[2];

  // the file path is just the version number + the end of the path
  var fileRequested =
    window.location.pathname.substring(
      window.location.pathname.lastIndexOf(currentVersion) +
      currentVersion.length);
  //alert(fileRequested);

  // without doing async loads, there is no way to know if the path actually
  // exists - so we will just have to load
  window.location = "/versions/" + newVersion + fileRequested;
}

function selectVersion(currentVersion) {
  document.getElementById("version-selector").value = currentVersion;
  //alert(currentVersion);
}
