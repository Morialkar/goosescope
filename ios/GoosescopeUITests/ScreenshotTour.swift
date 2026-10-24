import XCTest

/// Captures pour la validation visuelle (SPEC-IOS AC27). Lancé à la demande:
/// xcodebuild test -only-testing:GoosescopeUITests/ScreenshotTour. Les images sont dans le .xcresult.
final class ScreenshotTour: XCTestCase {
    func testTour() {
        let app = XCUIApplication()
        app.launchEnvironment["GOOSESCOPE_FAKE_CAMERA"] = "bands"
        app.launchArguments = ["-goosescope.lang", "fr"]
        app.launch()
        XCTAssertTrue(app.buttons["freeze"].waitForExistence(timeout: 5))
        snap(app, "1-direct")
        app.buttons["freeze"].tap()
        XCTAssertTrue(app.staticTexts["detailBase"].waitForExistence(timeout: 5))
        sleep(1)
        snap(app, "2-fiche")
        app.buttons["detailToggle"].tap()
        sleep(1)
        snap(app, "3-fiche-reduite")
    }

    private func snap(_ app: XCUIApplication, _ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = name
        shot.lifetime = .keepAlways
        add(shot)
    }
}
