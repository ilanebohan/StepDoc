package com.exodius_software.stepdoc.core.render;

/** Inlined assets of the HTML page. Kept apart so the renderer reads as structure only. */
final class HtmlAssets {

    static final String STYLE = """
            :root {
              color-scheme: light dark;
              --bg: #ffffff; --fg: #1f2328; --muted: #59636e; --border: #d1d9e0; --surface: #f6f8fa;
              --given: #0969da; --when: #8250df; --then: #1a7f37; --and: #59636e; --other: #9a6700;
            }
            @media (prefers-color-scheme: dark) {
              :root {
                --bg: #0d1117; --fg: #e6edf3; --muted: #9198a1; --border: #3d444d; --surface: #151b23;
                --given: #4493f8; --when: #ab7df8; --then: #3fb950; --and: #9198a1; --other: #d29922;
              }
            }
            * { box-sizing: border-box; }
            body { margin: 0; background: var(--bg); color: var(--fg);
              font: 15px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
            header { position: sticky; top: 0; z-index: 1; background: var(--bg);
              border-bottom: 1px solid var(--border); padding: 16px 32px; }
            header h1 { margin: 0; font-size: 24px; }
            .summary { margin: 4px 0 12px; color: var(--muted); }
            #filter { width: 100%; max-width: 480px; padding: 8px 12px; font: inherit; color: inherit;
              background: var(--surface); border: 1px solid var(--border); border-radius: 6px; }
            main { padding: 8px 32px 48px; }
            h2 { margin: 32px 0 4px; font-size: 19px; }
            .subtitle { margin: 0 0 12px; color: var(--muted); }
            table { width: 100%; border-collapse: collapse; }
            th, td { text-align: left; vertical-align: top; padding: 8px 12px; border-bottom: 1px solid var(--border); }
            th { font-size: 13px; color: var(--muted); font-weight: 600; background: var(--surface); }
            td p { margin: 0 0 8px; }
            td p:last-child { margin-bottom: 0; }
            code { font: 13px/1.4 ui-monospace, "Cascadia Code", Consolas, monospace; }
            .expression { white-space: pre-wrap; }
            .keyword { font-weight: 600; font-size: 13px; }
            .keyword-given { color: var(--given); } .keyword-when { color: var(--when); }
            .keyword-then { color: var(--then); } .keyword-and, .keyword-but { color: var(--and); }
            .keyword-other { color: var(--other); }
            .parameters { margin: 0; padding-left: 18px; }
            .parameters .name { font-weight: 600; }
            .examples { color: var(--muted); }
            .source { color: var(--muted); font-size: 13px; white-space: nowrap; }
            .problems li { margin-bottom: 4px; }
            #no-match { color: var(--muted); }
            """;

    /** Hides rows not containing the query, then sections left without rows. */
    static final String FILTER_SCRIPT = """
            const filter = document.getElementById('filter');
            if (filter) {
              filter.addEventListener('input', () => {
                const query = filter.value.trim().toLowerCase();
                let anyVisible = false;
                document.querySelectorAll('section.group').forEach(section => {
                  let visibleRows = 0;
                  section.querySelectorAll('tbody tr').forEach(row => {
                    const visible = !query || row.textContent.toLowerCase().includes(query);
                    row.hidden = !visible;
                    if (visible) visibleRows++;
                  });
                  section.hidden = visibleRows === 0;
                  anyVisible = anyVisible || visibleRows > 0;
                });
                document.getElementById('no-match').hidden = anyVisible;
              });
            }
            """;

    private HtmlAssets() {
    }
}
