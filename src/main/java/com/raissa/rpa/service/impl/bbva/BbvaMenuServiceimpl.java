package com.raissa.rpa.service.impl.bbva;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.raissa.rpa.exception.BbvaException;
import com.raissa.rpa.service.bbva.BbvaMenuService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@Slf4j
public class BbvaMenuServiceimpl implements BbvaMenuService {
    public boolean isMenuVisible(Page page) {
        int timeoutMs = 5_000;
        try {
            MetodsGeneric.randomWait(2000, 3000);
            Locator app =MetodsGeneric.waitForVisible(page, "bbva-btge-app-template#app__content", timeoutMs);

            Locator nav = app.locator("bbva-btge-sidebar-menu.sidebar[slot='nav']").first();
            nav.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED)
                    .setTimeout(timeoutMs));

            ElementHandle navEl = nav.elementHandle();
            if (navEl == null) return false;

            ElementHandle containerEl = page.evaluateHandle(
                    "(navMenu) => {" +
                            "  if (!navMenu || !navMenu.shadowRoot) return null;" +
                            "  const sr = navMenu.shadowRoot;" +
                            "  if (!sr) return null;" +
                            "  return sr.querySelector('div.container');" +
                            "}",
                    navEl
            ).asElement();
            if (containerEl == null) return false;

            ElementHandle menuEl = page.evaluateHandle(
                    "(container) => {" +
                            "  if (!container) return null;" +
                            "  return container.querySelector(\"bbva-web-navigation-menu[role='navigation'][aria-label='Main menu']\");" +
                            "}",
                    containerEl
            ).asElement();
            if (menuEl == null) return false;

            ElementHandle itemInicioEl = page.evaluateHandle(
                    "(menu) => {" +
                            "  if (!menu) return null;" +
                            "  return menu.querySelector(\"bbva-web-navigation-menu-item[event-name='event-111V00001']\");" +
                            "}",
                    menuEl
            ).asElement();
            if (itemInicioEl == null) return false;

            ElementHandle actionSelectedEl = page.evaluateHandle(
                    "(item) => {" +
                            "  if (!item || !item.shadowRoot) return null;" +
                            "  return item.shadowRoot.querySelector('bbva-web-navigation-menu-item-action[selected]');" +
                            "}",
                    itemInicioEl
            ).asElement();
            return actionSelectedEl != null;
        } catch (Exception e) {
            log.error("Error al tratar de ubicar el menu como validacion de ingreso");
            return false;
        }
    }

    public boolean clickCuentas(Page page) {
        int timeoutMs = 5_000;

        try {
            MetodsGeneric.waitForAttached(page, "bbva-btge-app-template#app__content", timeoutMs);

            ElementHandle buttonCuentas = page.evaluateHandle(
                    "() => {" +
                            "const appHost = document.querySelector('bbva-btge-app-template#app__content');" +

                            "const navMenu = appHost.querySelector(\"bbva-btge-sidebar-menu.sidebar[slot='nav']\");" +
                            "if (!navMenu || !navMenu.shadowRoot) return null;" +

                            "const container = navMenu.shadowRoot.querySelector('div.container');" +
                            "if (!container) return null;" +

                            "const sidebar = container.querySelector(\"bbva-web-navigation-menu[role='navigation'][aria-label='Main menu']\");" +

                            "const navMenuItem = sidebar.querySelector(\"bbva-web-navigation-menu-item[event-name='event-111V00002']\");" +
                            "if (!navMenuItem || !navMenuItem.shadowRoot) return null;" +

                            "return navMenuItem.shadowRoot.querySelector(\"bbva-web-navigation-menu-item-action[role='button']\");" +
                            "}"
            ).asElement();

            if (buttonCuentas == null) return false;

            try {
                buttonCuentas.click(new ElementHandle.ClickOptions().setTimeout(timeoutMs));
                return true;
            } catch (Exception e) {
                page.evaluate("(el) => el.click()", buttonCuentas);
                return true;
            }
        } catch (Exception e) {
            log.error("Error al tratar de hacer click en cuentas");
            return false;
        }
    }

    public boolean clickAllCuentas(Page page) {
        ElementHandle iframeEl = null;

        try {
            try {
                MetodsGeneric.randomWait(4_000, 5_000);
            } catch (Exception e) {
                log.error("Error al esperar para cerrar modal");
            }

            closeModalIfPresent(page);

            iframeEl = page.evaluateHandle(
                    "() => {" +
                            "const body = document.querySelector('bbva-btge-app-template#app__content');" +
                            "const landing = body.querySelector(" +
                            "'bbva-btge-accounts-solution-page#cells-template-bbva-btge-accounts-solution'" +
                            ");" +
                            "if (!landing || !landing.shadowRoot) return null;" +

                            "const panel = landing.shadowRoot.querySelector(" +
                            "\"cells-template-paper-drawer-panel[state='active']\"" +
                            ");" +
                            "if (!panel) return null;" +

                            "const appMain = panel.querySelector(\"div[slot='app__main'].container\");" +
                            "if (!appMain) return null;" +

                            "const iframeHost = appMain.querySelector(\"bbva-core-iframe[iframe-title='bbva-btge-accounts-solution']\");" +
                            "if (!iframeHost || !iframeHost.shadowRoot) return null;" +

                            "const iframeContainer = iframeHost.shadowRoot.querySelector('div#iframeContainer.iframe-container');" +
                            "if (!iframeContainer) return null;" +

                            "return iframeContainer.querySelector('iframe#bbvaIframe');" +
                            "}"
            ).asElement();

            if (iframeEl == null) return false;

            Frame iframe = iframeEl.contentFrame();
            if (iframe == null) return false;

            // Dentro del iframe, navegar shadow roots y click en "boton lista"
            iframe.evaluate(
                    "() => {" +
                            "const homeHost = document.querySelector(" +
                            "'bbva-btge-accounts-solution-home-page#cells-template-bbva-btge-accounts-solution-home'" +
                            ");" +
                            "if (!homeHost || !homeHost.shadowRoot) return;" +

                            "const panel = homeHost.shadowRoot.querySelector(" +
                            "\"cells-template-paper-drawer-panel[state='active']\"" +
                            ");" +
                            "if (!panel) return;" +

                            "const appMain = panel.querySelector(\"div[slot='app__main']\");" +
                            "if (!appMain) return;" +

                            "const mainContainer = appMain.querySelector('div.main-content');" +
                            "if (!mainContainer) return;" +

                            "const wrapper = mainContainer.querySelector('div.page-wrapper');" +
                            "if (!wrapper) return;" +

                            "const layout = wrapper.querySelector('bbva-foundations-grid-tools-layout.zone');" +
                            "if (!layout) return;" +

                            "const fewAccounts = layout.querySelector(\"div.o-few-accounts[slot='mainLayout']\");" +
                            "if (!fewAccounts) return;" +

                            "const fewAccountsHeader = fewAccounts.querySelector('div.o-few-accounts__header.o-few-accounts__header--spacing');" +
                            "if (!fewAccountsHeader) return;" +

                            "const buttonGroup = fewAccountsHeader.querySelector('bbva-button-group.o-few-accounts__button-group');" +
                            "if (!buttonGroup || !buttonGroup.shadowRoot) return;" +

                            "const listItem = buttonGroup.shadowRoot " +
                            "  ? buttonGroup.shadowRoot.querySelector(\"bbva-button-group-item[value='ListView']\") " +
                            "  : buttonGroup.querySelector(\"bbva-button-group-item[value='ListView']\");" +
                            "if (!listItem) return;" +

                            "try { listItem.click(); } catch (e) {}" +
                            "if (listItem.shadowRoot) {" +
                            "  const inner = listItem.shadowRoot.querySelector(\"div.wrapper, [role='radio'], button, .item\");" +
                            "  if (inner) { try { inner.click(); } catch (e) {} }" +
                            "}" +
                            "}"
            );

            MetodsGeneric.randomWait(1_000, 1_500);

            // Dentro del iframe, navegar shadow roots y click en la primera cuenta que encuentre
            iframe.evaluate(
                    "() => {" +
                            "const homeHost = document.querySelector(" +
                            "'bbva-btge-accounts-solution-home-page#cells-template-bbva-btge-accounts-solution-home'" +
                            ");" +
                            "if (!homeHost || !homeHost.shadowRoot) return;" +

                            "const panel = homeHost.shadowRoot.querySelector(" +
                            "\"cells-template-paper-drawer-panel[state='active']\"" +
                            ");" +
                            "if (!panel) return;" +

                            "const appMain = panel.querySelector(\"div[slot='app__main']\");" +
                            "if (!appMain) return;" +

                            "const mainContainer = appMain.querySelector('div.main-content');" +
                            "if (!mainContainer) return;" +

                            "const wrapper = mainContainer.querySelector('div.page-wrapper');" +
                            "if (!wrapper) return;" +

                            "const layout = wrapper.querySelector('bbva-foundations-grid-tools-layout.zone');" +
                            "if (!layout) return;" +

                            "const fewAccounts = layout.querySelector(\"div.o-few-accounts[slot='mainLayout']\");" +
                            "if (!fewAccounts) return;" +

                            "const accordions = Array.from(fewAccounts.querySelectorAll('bbva-expandable-accordion.entity-accordion'));" +
                            "if (!accordions.length) return;" +

                            "for (const accordion of accordions) {" +
                            "  const content = accordion.querySelector(\"div.expandable-content[slot='content']\");" +
                            "  if (!content) continue;" +

                            "  const tableHost = content.querySelector('bbva-btge-accounts-solution-table.accountsTable');" +
                            "  if (!tableHost) continue;" +

                            "  if (!tableHost.shadowRoot) continue;" +
                            "  const table = tableHost.shadowRoot.querySelector('table');" +
                            "  if (!table) continue;" +

                            "  const row = table.querySelector('tr.row');" +
                            "  if (!row) continue;" +
                            "  const cell = row.querySelector('td.cellText');" +
                            "  if (!cell) continue;" +

                            "  const bodyTextHost = cell.querySelector('bbva-table-body-text.accountDescription');" +
                            "  if (!bodyTextHost || !bodyTextHost.shadowRoot) continue;" +

                            "  const link = bodyTextHost.shadowRoot.querySelector('bbva-type-link.link[tabindex=\"0\"]');" +
                            "  if (!link) continue;" +

                            "  try { link.click(); } catch(e) {}" +
                            "  return;" +
                            "}" +
                            "}"
            );

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> extractAccounts(Page page) {
        Map<String, Object> result = new HashMap<>();
        int timeoutMs = 5_000;

        try {
            MetodsGeneric.randomWait(3_000, 4_000);
            MetodsGeneric.waitForAttached(page, "bbva-btge-app-template#app__content", timeoutMs);

            ElementHandle iframeEl = page.evaluateHandle(
                    "() => {" +
                            "const body = document.querySelector('bbva-btge-app-template#app__content');" +
                            "const landing = body.querySelector(" +
                            "'bbva-btge-accounts-solution-page#cells-template-bbva-btge-accounts-solution'" +
                            ");" +
                            "if (!landing || !landing.shadowRoot) return null;" +

                            "const panel = landing.shadowRoot.querySelector(" +
                            "\"cells-template-paper-drawer-panel[state='active']\"" +
                            ");" +
                            "if (!panel) return null;" +

                            "const appMain = panel.querySelector(\"div[slot='app__main'].container\");" +
                            "if (!appMain) return null;" +

                            "const iframeHost = appMain.querySelector(\"bbva-core-iframe[iframe-title='bbva-btge-accounts-solution']\");" +
                            "if (!iframeHost || !iframeHost.shadowRoot) return null;" +

                            "const iframeContainer = iframeHost.shadowRoot.querySelector('div#iframeContainer.iframe-container');" +
                            "if (!iframeContainer) return null;" +

                            "return iframeContainer.querySelector('iframe#bbvaIframe');" +
                            "}"
            ).asElement();

            if (iframeEl == null) {
                throw new BbvaException("No se encontró iframe#bbvaIframe (legacy)");
            }

            Frame iframe = iframeEl.contentFrame();
            if (iframe == null) {
                throw new BbvaException("No se pudo obtener contentFrame() de iframe#bbvaIframe");
            }

            // ========= 1) EXTRAER LISTA DE CUENTAS =========
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cuentas = (List<Map<String, Object>>) iframe.evaluate(
                    "() => {" +
                            "const out = [];" +
                            "const clean = s => (s || '').replace(/\\D/g, '');" +

                            "const app = document.querySelector('div#app__content');" +
                            "if (!app) return out;" +
                            "const mov = app.querySelector('bbva-btge-accounts-solution-movements-page#cells-template-bbva-btge-accounts-solution-movements');" +
                            "if (!mov || !mov.shadowRoot) return out;" +
                            "const panel = mov.shadowRoot.querySelector(\"cells-template-paper-drawer-panel[state='active']\");" +
                            "if (!panel) return out;" +
                            "const wrapper = panel.querySelector('div[slot=\"app__main\"] div.main-content div.page-wrapper');" +
                            "if (!wrapper) return out;" +
                            "const header = wrapper.querySelector('bbva-btge-web-header-product#transactionsHeaderProduct');" +
                            "if (!header) return out;" +

                            "const selectSlot = header.querySelector('div[slot=\"selectFilterProductSlot\"]');" +
                            "if (!selectSlot) return out;" +
                            "const selectHost = selectSlot.querySelector('bbva-form-select-filter');" +
                            "if (!selectHost) return out;" +

                            // IMPORTANTE: primero light DOM (ahí están tus options reales)
                            "let opts = Array.from(selectHost.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                            // fallback shadow solo si light no trajo nada útil
                            "if (!opts.length && selectHost.shadowRoot) {" +
                            "  opts = Array.from(selectHost.shadowRoot.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                            "}" +

                            "opts.forEach((o, idx) => {" +
                            "  const v = o.getAttribute('value');" +
                            "  const a = o.getAttribute('aria-label');" +
                            "  const t = (o.textContent || '').trim();" +
                            "  const raw = v || a || t;" +
                            "  const numero = clean(raw);" +
                            "  if (!numero) return;" + // evita wrappers vacíos
                            "  out.push({" +
                            "    index: idx," +
                            "    numeroCuenta: numero," +
                            "    selected: o.hasAttribute('selected') || o.getAttribute('aria-selected') === 'true'" +
                            "  });" +
                            "});" +

                            "return out;" +
                            "}"
            );

            if (cuentas == null || cuentas.isEmpty()) {
                throw new BbvaException("No se encontraron cuentas en el selector");
            }

            Supplier<Map<String, Object>> leerSaldosActuales = () -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> bal = (Map<String, Object>) iframe.evaluate(
                        "() => {" +
                                "const toCurrency = (v) => {" +
                                "  const c = (v || '').trim();" +
                                "  if (c === 'S/' || c.toUpperCase() === 'PEN') return 'PEN';" +
                                "  if (c === '$' || c.toUpperCase() === 'USD') return 'USD';" +
                                "  return c;" +
                                "};" +
                                "let saldoDisp = 0, saldoCont = 0, moneda = '';" +

                                "const app = document.querySelector('div#app__content');" +
                                "const mov = app?.querySelector('bbva-btge-accounts-solution-movements-page#cells-template-bbva-btge-accounts-solution-movements');" +
                                "const panel = mov?.shadowRoot?.querySelector(\"cells-template-paper-drawer-panel[state='active']\");" +
                                "const wrapper = panel?.querySelector('div[slot=\"app__main\"] div.main-content div.page-wrapper');" +
                                "const header = wrapper?.querySelector('bbva-btge-web-header-product#transactionsHeaderProduct');" +
                                "const mainData = header?.querySelector('div[slot=\"mainDataSlot\"]');" +
                                "if (!mainData) return { saldoDisp, saldoCont, moneda };" +

                                "const lis = Array.from(mainData.querySelectorAll('ul.c-header-product__amounts > li'));" +
                                "for (const li of lis) {" +
                                "  const simple = li.querySelector('bbva-btge-list-simple');" +
                                "  if (!simple) continue;" +
                                "  const sr = simple.shadowRoot || simple;" +
                                "  const titleEl = sr.querySelector('bbva-type-text[data-test-id=\"container-header-main-title\"]');" +
                                "  const amountEl = sr.querySelector('bbva-type-amount[data-test-id=\"container-header-right\"]');" +
                                "  if (!titleEl || !amountEl) continue;" +

                                "  const title = ((titleEl.getAttribute('text') || titleEl.textContent || '').trim()).toLowerCase();" +
                                "  const amount = parseFloat(amountEl.getAttribute('amount') || '0');" +
                                "  const curRaw = amountEl.getAttribute('currency-code') || amountEl.getAttribute('symbol') || '';" +
                                "  if (!moneda) moneda = toCurrency(curRaw);" +
                                "  if (title.includes('disponible')) saldoDisp = isNaN(amount) ? 0 : amount;" +
                                "  if (title.includes('contable')) saldoCont = isNaN(amount) ? 0 : amount;" +
                                "}" +
                                "return { saldoDisp, saldoCont, moneda };" +
                                "}"
                );
                return bal;
            };

            Consumer<Integer> seleccionarCuenta = (idx) -> {
                iframe.evaluate(
                        "(targetIdx) => {" +
                                "const app = document.querySelector('div#app__content');" +
                                "const mov = app?.querySelector('bbva-btge-accounts-solution-movements-page#cells-template-bbva-btge-accounts-solution-movements');" +
                                "const panel = mov?.shadowRoot?.querySelector(\"cells-template-paper-drawer-panel[state='active']\");" +
                                "const wrapper = panel?.querySelector('div[slot=\"app__main\"] div.main-content div.page-wrapper');" +
                                "const header = wrapper?.querySelector('bbva-btge-web-header-product#transactionsHeaderProduct');" +
                                "const selectHost = header?.querySelector('div[slot=\"selectFilterProductSlot\"] bbva-form-select-filter');" +
                                "if (!selectHost) return false;" +

                                "let opts = Array.from(selectHost.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                                "if (!opts.length && selectHost.shadowRoot) {" +
                                "  opts = Array.from(selectHost.shadowRoot.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                                "}" +
                                "const target = opts[targetIdx];" +
                                "if (!target) return false;" +

                                "opts.forEach(o => {" +
                                "  o.removeAttribute('selected');" +
                                "  o.setAttribute('aria-selected','false');" +
                                "  if ((o.getAttribute('ambient') || '').toLowerCase() === 'secondary') o.removeAttribute('ambient');" +
                                "});" +

                                "target.setAttribute('selected','');" +
                                "target.setAttribute('aria-selected','true');" +
                                "target.setAttribute('ambient','secondary');" +

                                "const newValue = target.getAttribute('value') || '';" +
                                "if (newValue) {" +
                                "  try { selectHost.value = newValue; } catch(e) {}" +
                                "  selectHost.setAttribute('value', newValue);" +
                                "}" +

                                "try { target.click(); } catch(e) {}" +
                                "selectHost.dispatchEvent(new Event('input', { bubbles:true, composed:true }));" +
                                "selectHost.dispatchEvent(new Event('change', { bubbles:true, composed:true }));" +
                                "selectHost.dispatchEvent(new CustomEvent('value-changed', { bubbles:true, composed:true, detail:{ value:newValue } }));" +
                                "return true;" +
                                "}",
                        idx
                );
            };

            // ========= 2) ORDENAR: seleccionada primero =========
            List<Map<String, Object>> ordered = new ArrayList<>();
            Map<String, Object> selected = cuentas.stream()
                    .filter(c -> Boolean.TRUE.equals(c.get("selected")))
                    .findFirst()
                    .orElse(cuentas.get(0));

            ordered.add(selected);
            for (Map<String, Object> c : cuentas) {
                if (c != selected) ordered.add(c);
            }

            // ========= 3) ITERAR Y EXTRAER =========
            List<Map<String, Object>> data = new ArrayList<>();

            for (int i = 0; i < ordered.size(); i++) {
                Map<String, Object> cta = ordered.get(i);
                Integer idx = ((Number) cta.get("index")).intValue();

                if (i > 0) {
                    String before = (String) iframe.evaluate(
                            "() => {" +
                                    "const app = document.querySelector('div#app__content');" +
                                    "const mov = app?.querySelector('bbva-btge-accounts-solution-movements-page#cells-template-bbva-btge-accounts-solution-movements');" +
                                    "const panel = mov?.shadowRoot?.querySelector(\"cells-template-paper-drawer-panel[state='active']\");" +
                                    "const wrapper = panel?.querySelector('div[slot=\"app__main\"] div.main-content div.page-wrapper');" +
                                    "const header = wrapper?.querySelector('bbva-btge-web-header-product#transactionsHeaderProduct');" +
                                    "const selectHost = header?.querySelector('div[slot=\"selectFilterProductSlot\"] bbva-form-select-filter');" +
                                    "if (!selectHost) return '';" +
                                    "let opts = Array.from(selectHost.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                                    "if (!opts.length && selectHost.shadowRoot) opts = Array.from(selectHost.shadowRoot.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                                    "const cur = opts.find(o => o.hasAttribute('selected') || o.getAttribute('aria-selected')==='true');" +
                                    "return cur ? (cur.getAttribute('value') || '') : '';" +
                                    "}"
                    );

                    seleccionarCuenta.accept(idx);

                    iframe.waitForFunction(
                            "(prev) => {" +
                                    "const app = document.querySelector('div#app__content');" +
                                    "const mov = app?.querySelector('bbva-btge-accounts-solution-movements-page#cells-template-bbva-btge-accounts-solution-movements');" +
                                    "const panel = mov?.shadowRoot?.querySelector(\"cells-template-paper-drawer-panel[state='active']\");" +
                                    "const wrapper = panel?.querySelector('div[slot=\"app__main\"] div.main-content div.page-wrapper');" +
                                    "const header = wrapper?.querySelector('bbva-btge-web-header-product#transactionsHeaderProduct');" +
                                    "const selectHost = header?.querySelector('div[slot=\"selectFilterProductSlot\"] bbva-form-select-filter');" +
                                    "if (!selectHost) return false;" +
                                    "let opts = Array.from(selectHost.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                                    "if (!opts.length && selectHost.shadowRoot) opts = Array.from(selectHost.shadowRoot.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                                    "const cur = opts.find(o => o.hasAttribute('selected') || o.getAttribute('aria-selected')==='true');" +
                                    "const now = cur ? (cur.getAttribute('value') || '') : '';" +
                                    "return now && now !== prev;" +
                                    "}",
                            before,
                            new Frame.WaitForFunctionOptions().setTimeout(4_000)
                    );
                }

                Map<String, Object> bal = leerSaldosActuales.get();

                Map<String, Object> fila = new HashMap<>();
                fila.put(Constantes.KEY_NUMERO_CUENTA, cta.get("numeroCuenta"));
                fila.put(Constantes.KEY_TIPO_CUENTA, "");
                fila.put(Constantes.KEY_SALDO_CONT, bal.get("saldoCont"));
                fila.put(Constantes.KEY_SALDO_DISP, bal.get("saldoDisp"));
                fila.put(Constantes.KEY_MONEDA, bal.get("moneda"));

                data.add(fila);
            }

            result.put("data", data);
            result.put("count", data.size());
            result.put(Constantes.KEY_SUCCESS, true);
            result.put(Constantes.KEY_MESSAGE, "Cuentas extraídas correctamente");
            return result;
        } catch (BbvaException e) {
            log.error("Error controlado al mapear contenido del iframe: {}", e.getMessage());
            result.put(Constantes.KEY_SUCCESS, false);
            result.put(Constantes.KEY_MESSAGE, e.getMessage());
            return result;
        } catch (Exception e) {
            log.error("Error no controlado al mapear contenido del iframe: {}", e.getMessage());
            result.put(Constantes.KEY_SUCCESS, false);
            result.put(Constantes.KEY_MESSAGE, e.getMessage());
            return result;
        }
    }

    public void busquedaMovimientos(Page page, String numeroCuenta, String fechaDesde, String fechaHasta) {
        int timeoutMs = 5_000;

        try {
            MetodsGeneric.randomWait(3_000, 4_000);
            MetodsGeneric.waitForAttached(page, "bbva-btge-app-template#app__content", timeoutMs);

            ElementHandle iframeEl = page.evaluateHandle(
                    "() => {" +
                            "const body = document.querySelector('bbva-btge-app-template#app__content');" +
                            "const landing = body.querySelector(" +
                            "'bbva-btge-accounts-solution-page#cells-template-bbva-btge-accounts-solution'" +
                            ");" +
                            "if (!landing || !landing.shadowRoot) return null;" +

                            "const panel = landing.shadowRoot.querySelector(" +
                            "\"cells-template-paper-drawer-panel[state='active']\"" +
                            ");" +
                            "if (!panel) return null;" +

                            "const appMain = panel.querySelector(\"div[slot='app__main'].container\");" +
                            "if (!appMain) return null;" +

                            "const iframeHost = appMain.querySelector(\"bbva-core-iframe[iframe-title='bbva-btge-accounts-solution']\");" +
                            "if (!iframeHost || !iframeHost.shadowRoot) return null;" +

                            "const iframeContainer = iframeHost.shadowRoot.querySelector('div#iframeContainer.iframe-container');" +
                            "if (!iframeContainer) return null;" +

                            "return iframeContainer.querySelector('iframe#bbvaIframe');" +
                            "}"
            ).asElement();

            if (iframeEl == null) {
                throw new BbvaException("No se encontró iframe#bbvaIframe (legacy)");
            }

            Frame iframe = iframeEl.contentFrame();
            if (iframe == null) {
                throw new BbvaException("No se pudo obtener contentFrame() de iframe#bbvaIframe");
            }

            // 1) Esperar wrapper de movimientos/filtros
            iframe.waitForFunction(
                    "() => {" +
                            "const app = document.querySelector('div#app__content');" +
                            "const mov = app?.querySelector('bbva-btge-accounts-solution-movements-page#cells-template-bbva-btge-accounts-solution-movements');" +
                            "const panel = mov?.shadowRoot?.querySelector(\"cells-template-paper-drawer-panel[state='active']\");" +
                            "const wrap = panel?.querySelector('bbva-btge-transactions-table-wrapper#accountTransactionsWrapper');" +
                            "return !!wrap;" +
                            "}",
                    null,
                    new Frame.WaitForFunctionOptions().setTimeout(timeoutMs)
            );

            final String cuentaTarget = (numeroCuenta == null) ? "" : numeroCuenta.replaceAll("\\D", "");

            // 2) Ubicar índice de cuenta en combo
            Integer idxTarget = (Integer) iframe.evaluate(
                    "(target) => {" +
                            "const clean = s => (s || '').replace(/\\D/g, '');" +
                            "const app = document.querySelector('div#app__content');" +
                            "const mov = app?.querySelector('bbva-btge-accounts-solution-movements-page#cells-template-bbva-btge-accounts-solution-movements');" +
                            "const panel = mov?.shadowRoot?.querySelector(\"cells-template-paper-drawer-panel[state='active']\");" +
                            "const wrapper = panel?.querySelector('div[slot=\"app__main\"] div.main-content div.page-wrapper');" +
                            "const header = wrapper?.querySelector('bbva-btge-web-header-product#transactionsHeaderProduct');" +
                            "const selectHost = header?.querySelector('div[slot=\"selectFilterProductSlot\"] bbva-form-select-filter');" +
                            "if (!selectHost) return -1;" +
                            "let opts = Array.from(selectHost.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                            "if (!opts.length && selectHost.shadowRoot) opts = Array.from(selectHost.shadowRoot.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                            "for (let i = 0; i < opts.length; i++) {" +
                            "  const o = opts[i];" +
                            "  const raw = o.getAttribute('value') || o.getAttribute('aria-label') || (o.textContent || '').trim();" +
                            "  if (clean(raw) === target) return i;" +
                            "}" +
                            "return -1;" +
                            "}",
                    cuentaTarget
            );

            if (idxTarget == null || idxTarget < 0) {
                throw new BbvaException("La cuenta " + numeroCuenta + " no existe en el combo de cuentas");
            }

            // 3) Seleccionar cuenta target
            iframe.evaluate(
                    "(targetIdx) => {" +
                            "const app = document.querySelector('div#app__content');" +
                            "const mov = app?.querySelector('bbva-btge-accounts-solution-movements-page#cells-template-bbva-btge-accounts-solution-movements');" +
                            "const panel = mov?.shadowRoot?.querySelector(\"cells-template-paper-drawer-panel[state='active']\");" +
                            "const wrapper = panel?.querySelector('div[slot=\"app__main\"] div.main-content div.page-wrapper');" +
                            "const header = wrapper?.querySelector('bbva-btge-web-header-product#transactionsHeaderProduct');" +
                            "const selectHost = header?.querySelector('div[slot=\"selectFilterProductSlot\"] bbva-form-select-filter');" +
                            "if (!selectHost) return false;" +
                            "let opts = Array.from(selectHost.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                            "if (!opts.length && selectHost.shadowRoot) opts = Array.from(selectHost.shadowRoot.querySelectorAll('bbva-form-filter-option[role=\"option\"]'));" +
                            "const target = opts[targetIdx];" +
                            "if (!target) return false;" +
                            "opts.forEach(o => {" +
                            "  o.removeAttribute('selected');" +
                            "  o.setAttribute('aria-selected','false');" +
                            "  if ((o.getAttribute('ambient') || '').toLowerCase() === 'secondary') o.removeAttribute('ambient');" +
                            "});" +
                            "target.setAttribute('selected','');" +
                            "target.setAttribute('aria-selected','true');" +
                            "target.setAttribute('ambient','secondary');" +
                            "const newValue = target.getAttribute('value') || '';" +
                            "if (newValue) {" +
                            "  try { selectHost.value = newValue; } catch(e) {}" +
                            "  selectHost.setAttribute('value', newValue);" +
                            "}" +
                            "try { target.click(); } catch(e) {}" +
                            "selectHost.dispatchEvent(new Event('input', { bubbles:true, composed:true }));" +
                            "selectHost.dispatchEvent(new Event('change', { bubbles:true, composed:true }));" +
                            "selectHost.dispatchEvent(new CustomEvent('value-changed', { bubbles:true, composed:true, detail:{ value:newValue } }));" +
                            "return true;" +
                            "}",
                    idxTarget
            );

            MetodsGeneric.randomWait(1_000, 2_000);

            // 4) Fechas con tipeo humanizado (solo dígitos por máscara dd/mm/yyyy)
            String fdDigits = (fechaDesde == null) ? "" : fechaDesde.replaceAll("\\D", "");
            String fhDigits = (fechaHasta == null) ? "" : fechaHasta.replaceAll("\\D", "");

            // Campo de texto previo (concepto/beneficiario...)
            Locator txtBusqueda = iframe.locator(
                    "bbva-btge-transactions-table-wrapper#accountTransactionsWrapper " +
                            "bbva-btge-table-filter bbva-form-input input[slot='_input']"
            ).first();

            Locator inFrom = iframe.locator(
                    "bbva-btge-transactions-table-wrapper#accountTransactionsWrapper " +
                            "bbva-btge-table-filter bbva-form-date-range input[name='from']"
            ).first();

            Locator inTo = iframe.locator(
                    "bbva-btge-transactions-table-wrapper#accountTransactionsWrapper " +
                            "bbva-btge-table-filter bbva-form-date-range input[name='to']"
            ).first();

            txtBusqueda.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(timeoutMs));
            inFrom.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(timeoutMs));
            inTo.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(timeoutMs));

            // 4.1 Click en caja de búsqueda + TAB para caer al primer date input
            txtBusqueda.click(new Locator.ClickOptions().setForce(true));
            MetodsGeneric.randomWaitPage(page, 80, 140);
            txtBusqueda.press("Tab");

            // 4.2 Escribir fecha desde (solo dígitos) -> UI auto avanza al segundo input
            MetodsGeneric.humanTypeWithKeyboard(page, fdDigits, 80, 160);

            // 4.3 Espera corta y escribir fecha hasta sin click adicional
            MetodsGeneric.randomWaitPage(page, 120, 220);
            MetodsGeneric.humanTypeWithKeyboard(page, fhDigits, 80, 160);

            // Opcional blur final suave
            MetodsGeneric.randomWaitPage(page, 120, 220);

            // 5) Click Buscar
            Locator btnBuscar = iframe.locator(
                    "bbva-btge-transactions-table-wrapper#accountTransactionsWrapper " +
                            "bbva-btge-table-filter bbva-button-default:has-text('Buscar')"
            ).first();
            btnBuscar.click(new Locator.ClickOptions().setTimeout(timeoutMs));

            // 6) Espera corta post-búsqueda (luego podemos cambiar por wait de grilla)
            iframe.waitForTimeout(700);
        } catch (BbvaException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al realizar busqueda por movimientos: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> extraerMovimientos(Page page) {
        List<Map<String, Object>> movimientos = new ArrayList<>();
        int timeoutMs = 5_000;

        try {
            MetodsGeneric.waitForAttached(page, "bbva-btge-app-template#app__content", timeoutMs);
            MetodsGeneric.randomWait(1_000, 2_000);

            ElementHandle iframeEl = page.evaluateHandle(
                    "() => {" +
                            "const body = document.querySelector('bbva-btge-app-template#app__content');" +
                            "const landing = body.querySelector(" +
                            "'bbva-btge-accounts-solution-page#cells-template-bbva-btge-accounts-solution'" +
                            ");" +
                            "if (!landing || !landing.shadowRoot) return null;" +

                            "const panel = landing.shadowRoot.querySelector(" +
                            "\"cells-template-paper-drawer-panel[state='active']\"" +
                            ");" +
                            "if (!panel) return null;" +

                            "const appMain = panel.querySelector(\"div[slot='app__main'].container\");" +
                            "if (!appMain) return null;" +

                            "const iframeHost = appMain.querySelector(\"bbva-core-iframe[iframe-title='bbva-btge-accounts-solution']\");" +
                            "if (!iframeHost || !iframeHost.shadowRoot) return null;" +

                            "const iframeContainer = iframeHost.shadowRoot.querySelector('div#iframeContainer.iframe-container');" +
                            "if (!iframeContainer) return null;" +

                            "return iframeContainer.querySelector('iframe#bbvaIframe');" +
                            "}"
            ).asElement();

            if (iframeEl == null) {
                throw new BbvaException("No se encontró iframe#bbvaIframe (legacy)");
            }

            Frame iframe = iframeEl.contentFrame();
            if (iframe == null) {
                throw new BbvaException("No se pudo obtener contentFrame() de iframe#bbvaIframe");
            }

            // 1) Expandir todos los "Ver más" (si existen) en bucle
            log.info("[EXTRACCION] tabla encontrada, inicio expandir ver mas");

            for (int i = 0; i < 15; i++) {
                Locator tfoot = iframe.locator("bbva-btge-accounts-solution-table#moviments-table table tfoot").first();
                if (tfoot.count() == 0) {
                    log.info("[EXTRACCION] no existe tfoot, fin paginacion");
                    break;
                }

                String txt = tfoot.innerText().toLowerCase();
                if (!(txt.contains("ver más") || txt.contains("ver mas"))) {
                    log.info("[EXTRACCION] no hay 'ver más', fin paginacion");
                    break;
                }

                Locator verMas = tfoot.locator("bbva-type-link, [role='button'], button, a, bbva-table-footer").first();
                if (verMas.count() == 0) {
                    log.info("[EXTRACCION] tfoot existe pero no hay clickable");
                    break;
                }

                int before = iframe.locator("bbva-btge-accounts-solution-table#moviments-table table tbody tr").count();
                verMas.click(new Locator.ClickOptions().setForce(true).setTimeout(timeoutMs));
                iframe.waitForTimeout(500);

                int after = iframe.locator("bbva-btge-accounts-solution-table#moviments-table table tbody tr").count();
                log.info("[EXTRACCION] click ver mas #{} | rows before={} after={}", i + 1, before, after);

                // seguridad: si no crece, corta para evitar loop infinito
                if (after <= before) break;
            }

            log.info("[EXTRACCION] fin expandir ver mas, inicio parse tbody");

            Locator rows = iframe.locator("bbva-btge-accounts-solution-table#moviments-table table tbody tr");
            rows.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED)
                    .setTimeout(timeoutMs));

            int n = rows.count();
            log.info("rows count={}", n);

            // 2) Extraer filas de tbody con tu formato
            for (int i = 0; i < n; i++) {
                Locator row = rows.nth(i);
                if (row.locator("td").count() < 5) continue;

                String day = safeAttr(row.locator("bbva-table-body-date.operationDate").first(), "date");
                String year = safeAttr(row.locator("bbva-table-body-date.operationDate").first(), "year");
                String fecha = toFecha(day, year); // dd/MM/yyyy

                String conceptMain = safeAttr(row.locator("bbva-table-body-text.concept").first(), "text");
                String conceptSub  = safeAttr(row.locator("bbva-table-body-text.concept").first(), "description");
                String descripcion = (conceptMain + " " + conceptSub).replaceAll("\\s+", " ").trim();

                String numMovRaw = safeAttr(row.locator("bbva-table-body-text.numberMovement").first(), "text");
                String operacion = MetodsGeneric.completarADiezDigitos(
                        numMovRaw == null ? "" : numMovRaw.replaceAll("\\D", "")
                );

                String amountStr = safeAttr(row.locator("bbva-table-body-amount.transactionAmount").first(), "amount");
                double montoRaw = 0d;
                try { montoRaw = Double.parseDouble(amountStr); } catch (Exception ignored) {}

                Map<String, Object> mov = new HashMap<>();
                mov.put(Constantes.KEY_FECHA, fecha);
                mov.put(Constantes.KEY_FECHA_VALOR, "");
                mov.put(Constantes.KEY_DESCRIPCION, descripcion);
                mov.put(Constantes.KEY_OPERACION, operacion);
                mov.put(Constantes.KEY_MONTO, Math.abs(montoRaw));
                mov.put(Constantes.KEY_TIPO, montoRaw < 0 ? Constantes.VALOR_DEBITO : Constantes.VALOR_CREDITO);
                mov.put(Constantes.KEY_SALDO, "");
                mov.put(Constantes.KEY_REFERENCIA, "");
                movimientos.add(mov);
            }
            log.info("se extrajo movimientos");
        } catch (Exception e) {
            log.error("Error al realizar busqueda por movimientos: {}", e.getMessage());
        }

        return movimientos;
    }

    public List<Map<String, Object>> extraerDetalleMovimientos(Page page, List<Map<String, Object>> listMovements) {
        List<Map<String, Object>> movimientosConDetalle = new ArrayList<>();
        //WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            /*WebElement tabla = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.tb_data")));
            Map<String, RowInfo> mapaDom = mapearFilasListado(driver, tabla);

            for (Map<String, Object> mov : listMovements) {
                String op = String.valueOf(mov.get(Constantes.KEY_OPERACION)).trim();

                RowInfo info = mapaDom.get(op);
                if (info == null) {
                    log.warn("Operación {} no encontrada en la tabla actual.", op);
                    continue;
                }

                int maxRetries = 2;
                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    try {
                        Map<String, Object> detalle = abrirYExtraerDetalleConVolver(driver, info, wait);

                        Map<String, Object> combinado = new HashMap<>(mov);
                        combinado.put(Constantes.KEY_FECHA_VALOR, detalle.get(Constantes.KEY_FECHA_VALOR));
                        combinado.put(Constantes.KEY_REFERENCIA, detalle.get(Constantes.KEY_FECHA_HORA));

                        movimientosConDetalle.add(combinado);

                        tabla = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.tb_data")));
                        mapaDom = mapearFilasListado(driver, tabla);

                        break;
                    } catch (StaleElementReferenceException | TimeoutException | NoSuchElementException ex) {
                        log.warn("Fallo detalle operacion={} intento={} err={}", op, attempt, ex.getMessage());
                        MetodsGeneric.randomWait(500, 1000);
                        if (attempt == maxRetries) {
                            Map<String, Object> combinado = new HashMap<>(mov);
                            combinado.put("detalle_error", ex.getMessage());
                            movimientosConDetalle.add(combinado);
                            try {
                                tabla = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.tb_data")));
                                mapaDom = mapearFilasListado(driver, tabla);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }*/
        } catch (Exception e) {
            log.error("Error al realizar busqueda por movimientos: {}", e.getMessage());
        }

        return movimientosConDetalle;
    }

    public boolean clickSalir(Page page) {
        int timeoutMs = 5_000;
        try {
            Locator host = page.locator("bbva-btge-sidebar-menu.sidebar[slot='nav']").first();
            host.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED)
                    .setTimeout(timeoutMs));

            // 1) Intento directo por evaluate en shadow roots
            Boolean clickedByEval = (Boolean) page.evaluate("""
                () => {
                  const host = document.querySelector("bbva-btge-sidebar-menu.sidebar[slot='nav']");
                  if (!host || !host.shadowRoot) return false;
                
                  const nav = host.shadowRoot.querySelector("bbva-web-navigation-menu[aria-label='Main menu']");
                  if (!nav) return false;
                
                  const root = nav.shadowRoot || nav;
                  let exit = root.querySelector("bbva-web-navigation-menu-item-action[item-action='exit']");
                  if (!exit) {
                    exit = root.querySelector("bbva-web-navigation-menu-item-action[variant='exit'], bbva-web-navigation-menu-item-action[class*='exit']");
                  }
                  if (!exit) return false;
                
                  exit.click();
                  return true;
                }
                """);
            if (Boolean.TRUE.equals(clickedByEval)) return true;

            // 2) Fallback: click por coordenada (simula tu click manual en hotspot)
            BoundingBox box = host.boundingBox();
            if (box != null) {
                // Ajusta estos offsets si quieres; aquí apunta a zona baja del menú donde suele estar "Salir"
                double x = box.x + Math.min(120, box.width * 0.55);
                double y = box.y + Math.max(40, box.height - 28);

                page.mouse().move(x, y);
                page.waitForTimeout(120);
                page.mouse().click(x, y);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.warn("clickSalir falló: {}", e.getMessage());
            return false;
        }
    }

    public boolean clickCerrarSesionModal(Page page) {
        int timeoutMs = 5_000;

        try {
            MetodsGeneric.randomWait(1_000, 2_000);

            page.locator("text=¿Deseas cerrar la sesión?").first()
                    .waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.ATTACHED)
                            .setTimeout(timeoutMs));

            // 2) Intento directo por rol/texto del botón visible
            try {
                Locator btn = page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Cerrar sesión")).first();
                btn.click(new Locator.ClickOptions().setTimeout(2_500));
                return true;
            } catch (Exception ignore) {}

            // 3) Fallback por locator de texto (incluye web components)
            try {
                Locator btnText = page.locator("text=Cerrar sesión").first();
                btnText.click(new Locator.ClickOptions().setForce(true).setTimeout(2_500));
                return true;
            } catch (Exception ignore) {}

            // 4) Fallback fuerte: evaluate buscando botón por texto dentro de TODOS los shadow roots
            Boolean clicked = (Boolean) page.evaluate(
                    "() => {" +
                            "const norm = s => (s||'').replace(/\\s+/g,' ').trim().toLowerCase();" +
                            "const wanted = 'cerrar sesión';" +
                            "const roots = [document];" +
                            "const seen = new Set();" +
                            "while (roots.length) {" +
                            "  const root = roots.shift();" +
                            "  if (!root || seen.has(root)) continue;" +
                            "  seen.add(root);" +
                            "  const all = root.querySelectorAll('*');" +
                            "  for (const el of all) {" +
                            "    if (el.shadowRoot) roots.push(el.shadowRoot);" +
                            "    const tag = (el.tagName || '').toLowerCase();" +
                            "    const txt = norm(el.textContent);" +
                            "    if ((tag.includes('button') || el.getAttribute('role') === 'button') && txt.includes(wanted)) {" +
                            "      el.click();" +
                            "      return true;" +
                            "    }" +
                            "  }" +
                            "}" +
                            "return false;" +
                            "}"
            );

            return Boolean.TRUE.equals(clicked);

        } catch (Exception e) {
            log.warn("clickCerrarSesionModal falló: {}", e.getMessage());
            return false;
        }
    }

/*    private Map<String, Object> abrirYExtraerDetalleConVolver(WebDriver driver, RowInfo info, WebDriverWait wait) throws InterruptedException {
        Map<String, Object> detalle = null;
        try {
            info.linkElem.click();
        } catch (Exception clickEx) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", info.linkElem);
        }
        MetodsGeneric.randomWait(500, 1000);
        WebElement tbInfo = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("table.tb_info")
        ));

        detalle = parsearDetalleDesdeTbInfo(driver, tbInfo);

        WebElement volver = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a.bt_return.pdfexportnone")
        ));
        volver.click();

        MetodsGeneric.randomWait(500, 1000);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.tb_data")));

        return detalle;
    }

    private Map<String, Object> parsearDetalleDesdeTbInfo(WebDriver driver, WebElement tbInfo) {
        Map<String, Object> detalle = new HashMap<>();

        try {
            WebElement h3 = driver.findElement(By.xpath("//div[@class='info']/h3"));
            String titulo = normalizar(h3.getText());
            String op = extraerOperacionDesdeTitulo(titulo);
            if (op != null) detalle.put("operacion", op);
        } catch (NoSuchElementException ignored) {}

        List<WebElement> filas = tbInfo.findElements(By.cssSelector("tbody > tr"));
        for (WebElement tr : filas) {
            List<WebElement> celdas = tr.findElements(By.cssSelector("td"));

            String llave = normalizar(celdas.get(0).getText());
            String valor = normalizar(celdas.get(1).getText());

            switch (llave) {
                case "Fecha Valor :":
                case "Fecha Valor:":
                    detalle.put(Constantes.KEY_FECHA_VALOR, valor);
                    break;
                case "Fecha y Hora de Operación :":
                case "Fecha y Hora de Operacion :":
                case "Fecha y Hora de Operación:":
                    detalle.put(Constantes.KEY_FECHA_HORA, valor);
                    break;
                default:
                    break;
            }
        }

        return detalle;
    }*/

    /**
     * Cierra un modal si es que se encuentra
     * @param page manejador de página
     */
    public void closeModalIfPresent(Page page) {
        try {
            int timeoutMs = 10_000;
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            MetodsGeneric.randomWait(3_000, 4_000);
            MetodsGeneric.waitForVisible(page, "bbva-btge-app-template#app__content", timeoutMs);

            Locator host = page.locator("bbva-btge-microfrontend-modal[opened]").first();
            if (host.count() == 0) return;

            host.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED)
                    .setTimeout(timeoutMs));

            MetodsGeneric.randomWaitPage(page, 500, 1_000);

            Boolean clicked = (Boolean) page.evaluate(
                    "() => {" +
                            "  const host = document.querySelector('bbva-btge-microfrontend-modal[opened]');" +
                            "  if (!host || !host.shadowRoot) return false;" +
                            "  const template = host.shadowRoot.querySelector('bbva-web-template-modal');" +
                            "  if (!template || !template.shadowRoot) return false;" +
                            "  const btn = template.shadowRoot.querySelector('button.close-btn');" +
                            "  if (!btn) return false;" +
                            "  btn.click();" +
                            "  return true;" +
                            "}"
            );

            if (!Boolean.TRUE.equals(clicked)) {
                log.warn("No se encontró button.close-btn dentro de shadow roots");
                return;
            }

            try {
                host.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(3_000));
            } catch (Exception ignore) {
                host.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.DETACHED)
                        .setTimeout(3_000));
            }
        } catch (Exception e) {
            log.error("Error al cerrar modal: {}", e.getMessage());
        }
    }

    /**
     * Devuelve un resultado de una fila html de un locator segun attr
     *
     * @param loc elemento
     * @param attr nombre de atributo
     * @return {@link String}
     */
    private String safeAttr(Locator loc, String attr) {
        try {
            if (loc.count() == 0) return "";
            String v = loc.getAttribute(attr);
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Devuelve una fecha formateada
     * @param dayMon Dia y mes 03 Mar
     * @param year año
     * @return {@link String}
     */
    private String toFecha(String dayMon, String year) {
        // dayMon ejemplo: "11 Mar"
        if (dayMon == null) dayMon = "";
        if (year == null) year = "";
        String[] p = dayMon.trim().toLowerCase().split("\\s+");
        if (p.length < 2 || year.isBlank()) return (dayMon + " " + year).trim();

        String dd = p[0].length() == 1 ? "0" + p[0] : p[0];
        String mm = switch (p[1]) {
            case "ene" -> "01";
            case "feb" -> "02";
            case "mar" -> "03";
            case "abr" -> "04";
            case "may" -> "05";
            case "jun" -> "06";
            case "jul" -> "07";
            case "ago" -> "08";
            case "sep" -> "09";
            case "oct" -> "10";
            case "nov" -> "11";
            case "dic" -> "12";
            default -> p[1];
        };
        return dd + "/" + mm + "/" + year.trim();
    }
}