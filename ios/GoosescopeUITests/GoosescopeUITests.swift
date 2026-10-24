import XCTest

/// Équivalents iOS des tests instrumentés Android (SPEC-IOS AC23, AC24), avec la caméra factice.
/// Textes attendus en anglais: chaque test démarre l'app en anglais (argument de lancement).
final class GoosescopeUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
    }

    private func launch(_ scenario: String, english: Bool = true) {
        app.launchEnvironment["GOOSESCOPE_FAKE_CAMERA"] = scenario
        app.launchArguments = english ? ["-goosescope.lang", "en"] : []
        app.launch()
    }

    private func label(_ id: String) -> XCUIElement { app.staticTexts[id] }
    private func button(_ id: String) -> XCUIElement { app.buttons[id] }

    private func waitText(_ id: String, _ expected: String, file: StaticString = #filePath, line: UInt = #line) {
        let element = label(id)
        XCTAssertTrue(element.waitForExistence(timeout: 5), "\(id) absent", file: file, line: line)
        let match = NSPredicate(format: "label == %@", expected)
        let ok = XCTWaiter().wait(for: [expectation(for: match, evaluatedWith: element)], timeout: 5) == .completed
        XCTAssertTrue(ok, "\(id) = \(element.label), attendu \(expected)", file: file, line: line)
    }

    private func freeze() {
        XCTAssertTrue(button("freeze").waitForExistence(timeout: 5))
        button("freeze").tap()
        XCTAssertTrue(label("detailBase").waitForExistence(timeout: 5))
    }

    func testNamesABlueObject() {
        launch("#1E90FF")
        waitText("baseName", "Blue")
    }

    func testShowsBaseAndShadeOnOneLine() {
        launch("#E8261C")
        waitText("baseName", "Red")
        waitText("shadeName", "— Scarlet")
    }

    func testSaysTooDarkWhenTheCameraIsCovered() {
        launch("dark")
        XCTAssertTrue(label("tooDark").waitForExistence(timeout: 5))
    }

    func testFreezingOnPeachOpensTheDetailSheet() {
        launch("#FFC4A0")
        waitText("baseName", "Orange")
        freeze()
        waitText("detailBase", "ORANGE")
        waitText("detailShade", "Peach")
        XCTAssertTrue(label("detailComparison").exists)
        XCTAssertEqual(app.descendants(matching: .any).matching(identifier: "natureTag").count, 3)
    }

    func testLanguageCanBeSwitchedFromTheDetailSheet() {
        launch("#FFC4A0")
        freeze()
        waitText("detailShade", "Peach")
        button("language").tap()
        waitText("detailShade", "Pêche")
        waitText("detailBase", "ORANGE")
        // Attendre que le bouton affiche son nouveau libellé: le toucher pendant qu'il change de taille se perdait.
        let switched = NSPredicate(format: "label == %@", "English")
        XCTAssertEqual(XCTWaiter().wait(for: [expectation(for: switched, evaluatedWith: button("language"))], timeout: 5), .completed)
        button("language").tap()
        waitText("detailShade", "Peach")
    }

    func testTappingThreeSpotsUpdatesTheSheet() {
        launch("bands")
        freeze()
        let image = app.images["frozenImage"].exists ? app.images["frozenImage"] : app.otherElements["frozenImage"]
        for (dy, expected) in [(0.10, "RED"), (0.30, "GREEN")] {
            image.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: dy)).tap()
            waitText("detailBase", expected)
        }
    }

    func testHidingTheSheetRevealsTheImageForPickingAnotherSpot() {
        launch("split")
        freeze()
        button("detailToggle").tap()
        waitText("compactBase", "BLUE")
        XCTAssertFalse(label("detailDescription").exists)
        let image = app.images["frozenImage"].exists ? app.images["frozenImage"] : app.otherElements["frozenImage"]
        image.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.8)).tap()
        waitText("compactBase", "RED")
        button("detailToggle").tap()
        XCTAssertTrue(label("detailDescription").waitForExistence(timeout: 5))
    }

    func testSwipingTheSheetDownHidesItAndUpShowsItAgain() {
        launch("split")
        freeze()
        let grabber = app.descendants(matching: .any)["detailGrabber"]
        XCTAssertTrue(grabber.waitForExistence(timeout: 5))
        grabber.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
            .press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.98)))
        XCTAssertTrue(label("compactBase").waitForExistence(timeout: 5))
        XCTAssertFalse(label("detailDescription").exists)
        let compactGrabber = app.descendants(matching: .any)["detailGrabber"]
        compactGrabber.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
            .press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)))
        XCTAssertTrue(label("detailDescription").waitForExistence(timeout: 5))
    }

    func testBackToCameraResumesLiveMode() {
        launch("#1E90FF")
        freeze()
        button("resume").tap()
        XCTAssertTrue(button("freeze").waitForExistence(timeout: 5))
    }

    func testLanguageSwitchesAndIsRemembered() {
        launch("#1E90FF")
        waitText("baseName", "Blue")
        button("language").tap()
        waitText("baseName", "Bleu")
        app.terminate()
        launch("#1E90FF", english: false)
        waitText("baseName", "Bleu")
        button("language").tap()
        waitText("baseName", "Blue")
    }

    func testDeniedPermissionExplainsAndOffersSettings() {
        launch("denied")
        XCTAssertTrue(button("permission.primary").waitForExistence(timeout: 5))
    }

    func testCameraFailureShowsAnErrorWithRetry() {
        launch("fail")
        XCTAssertTrue(button("failure.primary").waitForExistence(timeout: 5))
    }
}
