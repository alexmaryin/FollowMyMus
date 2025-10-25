import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        StartHelperKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}