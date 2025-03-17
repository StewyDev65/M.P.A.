public class AppStyles {
    // CSS for dark mode styling
    public static final String DARK_THEME_CSS = """
        .root {
            -fx-background-color: #1e1e1e;
            -fx-font-family: 'Segoe UI', Arial, sans-serif;
        }
       \s
        .label {
            -fx-text-fill: #e1e1e1;
        }
       \s
        .button {
            -fx-background-color: #3c3f41;
            -fx-text-fill: #e1e1e1;
            -fx-background-radius: 4px;
            -fx-border-radius: 4px;
            -fx-padding: 6px 12px;
            -fx-cursor: hand;
        }
       \s
        .button:hover {
            -fx-background-color: #494d50;
        }
       \s
        .button:pressed {
            -fx-background-color: #5a5d60;
        }
       \s
        .text-field, .combo-box {
            -fx-background-color: #3c3f41;
            -fx-text-fill: #e1e1e1;
            -fx-prompt-text-fill: #9e9e9e;
            -fx-background-radius: 4px;
            -fx-border-color: #5a5d60;
            -fx-border-radius: 4px;
        }
       \s
        .scroll-pane, .scroll-pane > .viewport {
            -fx-background-color: #2d2d2d;
        }
       \s
        .scroll-bar {
            -fx-background-color: #3c3f41;
        }
       \s
        .scroll-bar .thumb {
            -fx-background-color: #5a5d60;
        }
       \s
        .menu-bar {
            -fx-background-color: #1e1e1e;
            -fx-border-color: #5a5d60;
            -fx-border-width: 0 0 1 0;
        }
       \s
        .menu, .menu-item, .context-menu {
            -fx-background-color: #1e1e1e;
        }
       \s
        .menu .label, .menu-item .label {
            -fx-text-fill: #e1e1e1;
        }
       \s
        .menu-item:focused, .menu-item:hover {
            -fx-background-color: #3c3f41;
        }
       \s
        .separator .line {
            -fx-border-color: #5a5d60;
        }
       \s
        .slider .track {
            -fx-background-color: #3c3f41;
        }
       \s
        .slider .thumb {
            -fx-background-color: #56a0d3;
        }
       \s
        .check-box .box {
            -fx-background-color: #3c3f41;
            -fx-border-color: #5a5d60;
        }
       \s
        .check-box:selected .mark {
            -fx-background-color: #56a0d3;
        }
       \s
        .progress-bar > .track {
            -fx-background-color: #3c3f41;
        }
       \s
        .progress-bar > .bar {
            -fx-background-color: #56a0d3;
        }
       \s
        .table-view {
            -fx-background-color: #2d2d2d;
            -fx-border-color: #5a5d60;
        }
       \s
        .table-view .column-header-background {
            -fx-background-color: #1e1e1e;
        }
       \s
        .table-view .column-header, .table-view .filler {
            -fx-background-color: #3c3f41;
            -fx-text-fill: #e1e1e1;
        }
       \s
        .table-row-cell {
            -fx-background-color: #2d2d2d;
            -fx-text-fill: #e1e1e1;
        }
       \s
        .table-row-cell:odd {
            -fx-background-color: #333333;
        }
       \s
        .table-row-cell:selected {
            -fx-background-color: #4e4e4e;
        }
       \s
        .preview-container {
            -fx-background-color: #2d2d2d;
            -fx-border-color: #5a5d60;
            -fx-border-width: 1;
            -fx-border-radius: 4px;
            -fx-padding: 10px;
        }
       \s
        .controls-container {
            -fx-background-color: #2d2d2d;
            -fx-border-color: #5a5d60;
            -fx-border-width: 1;
            -fx-border-radius: 4px;
            -fx-padding: 15px;
        }
       \s
        .info-label {
            -fx-font-size: 12px;
            -fx-text-fill: #9e9e9e;
        }
       \s
        .title-label {
            -fx-font-size: 18px;
            -fx-font-weight: bold;
        }
       \s
        .section-header {
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-border-color: #5a5d60;
            -fx-border-width: 0 0 1 0;
            -fx-padding: 0 0 5 0;
            -fx-margin: 0 0 5 0;
        }
   \s""";
}