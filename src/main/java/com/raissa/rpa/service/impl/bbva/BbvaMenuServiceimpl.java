package com.raissa.rpa.service.impl.bbva;

import com.raissa.rpa.service.bbva.BbvaMenuService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.DateUtils;
import com.raissa.rpa.util.MetodsGeneric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BbvaMenuServiceimpl implements BbvaMenuService {
    /*public boolean isMenuVisible(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement appHost = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("bbva-btge-app-template#app__content")
            ));

            WebElement navMenu = appHost.findElement(By.cssSelector("bbva-btge-sidebar-menu.sidebar[slot='nav']"));

            SearchContext s1 = navMenu.getShadowRoot();

            WebElement container;
            try {
                container = s1.findElement(By.cssSelector("div.container"));
            } catch (NoSuchElementException e) {
                container = navMenu.findElement(By.cssSelector("div.container"));
            }

            WebElement sidebar = container.findElement(By.cssSelector("bbva-web-navigation-menu[role='navigation'][aria-label='Main menu']"));

            WebElement navMenuItem = sidebar.findElement(By.cssSelector("bbva-web-navigation-menu-item[event-name='event-111V00001']"));

            WebElement span = navMenuItem.findElement(By.cssSelector("span"));
            String text = span.getText().replace('\u00A0',' ').trim();
            return "Inicio".equals(text);
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    public boolean clickCuentas(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement appHost = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("bbva-btge-app-template#app__content")
            ));

            WebElement navMenu = appHost.findElement(By.cssSelector("bbva-btge-sidebar-menu.sidebar[slot='nav']"));

            SearchContext s1 = navMenu.getShadowRoot();

            WebElement container;
            try {
                container = s1.findElement(By.cssSelector("div.container"));
            } catch (NoSuchElementException e) {
                container = navMenu.findElement(By.cssSelector("div.container"));
            }

            WebElement sidebar = container.findElement(By.cssSelector("bbva-web-navigation-menu[role='navigation'][aria-label='Main menu']"));

            WebElement navMenuItem = sidebar.findElement(By.cssSelector("bbva-web-navigation-menu-item[event-name='event-111V00002']"));

            SearchContext s2 = navMenuItem.getShadowRoot();

            WebElement buttonCuentas = s2.findElement(By.cssSelector("bbva-web-navigation-menu-item-action[role='button']"));

            wait.until(ExpectedConditions.visibilityOf(buttonCuentas));
            try {
                wait.until(ExpectedConditions.elementToBeClickable(buttonCuentas)).click();
                return true;
            } catch (ElementNotInteractableException e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", buttonCuentas);
                return false;
            }
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    public boolean clickPosicionGlobalOnline(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("bbva-btge-app-template#app__content")
            ));

            WebElement landing = body.findElement(By.cssSelector(
                    "bbva-btge-menurization-landing-solution-page#cells-template-bbva-btge-menurization-landing-solution"
            ));
            SearchContext s1 = landing.getShadowRoot();

            WebElement navMenu = s1.findElement(By.cssSelector(
                    "cells-template-paper-drawer-panel.state-is-visible[state='active']"
            ));
            WebElement appMain = navMenu.findElement(By.cssSelector("div[slot='app__main'].container"));

            WebElement iframeHost = appMain.findElement(
                    By.cssSelector("bbva-core-iframe.microfrontend-iframe")
            );

            SearchContext s2 = iframeHost.getShadowRoot();

            WebElement iframeContainer = s2.findElement(By.cssSelector("div#iframeContainer.iframe-container"));
            WebElement iframe = iframeContainer.findElement(By.cssSelector("iframe#bbvaIframe"));
            driver.switchTo().frame(iframe);
            try {
                WebElement homeHost = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("bbva-btge-menurization-landing-solution-home-page#cells-template-bbva-btge-menurization-landing-solution-home")
                ));
                SearchContext s4 = homeHost.getShadowRoot();

                WebElement mainContainer = s4.findElement(By.cssSelector("div[slot='app__main'].main-container"));
                WebElement wrapper = mainContainer.findElement(By.cssSelector("div.wrapper"));
                WebElement layout = wrapper.findElement(By.cssSelector("bbva-foundations-grid-default-layout.content-layout.container-cards"));
                WebElement leftCol = layout.findElement(By.cssSelector("div.column[slot='left']"));
                WebElement sectionCard = leftCol.findElement(By.cssSelector("div.section-card"));
                WebElement contentLevel = sectionCard.findElement(By.cssSelector("div.content-level"));

                WebElement link = contentLevel.findElement(
                        By.xpath(".//bbva-web-link[@class='link-level' and @role='button'][normalize-space(.)='Posición global online']")
                );

                wait.until(ExpectedConditions.visibilityOf(link));
                try {
                    wait.until(ExpectedConditions.elementToBeClickable(link)).click();
                } catch (ElementNotInteractableException e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
                }
            } finally {
                driver.switchTo().defaultContent();
            }
            return true;
        } catch (TimeoutException | NoSuchElementException e) {
            try { driver.switchTo().defaultContent(); } catch (Exception ignore) {}
            return false;
        }
    }

    public Map<String, Object> extractAccounts(WebDriver driver) {
        Map<String, Object> result = new HashMap<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("bbva-btge-app-template#app__content")
            ));
            WebElement landing = body.findElement(By.cssSelector("legacy-page#cells-template-legacy"));
            SearchContext s1 = landing.getShadowRoot();

            WebElement navMenu = s1.findElement(By.cssSelector("cells-template-paper-drawer-panel.state-is-visible[state='active']"));
            WebElement appMain = navMenu.findElement(By.cssSelector("div[slot='app__main'].container"));
            WebElement iframeHost = appMain.findElement(
                    By.cssSelector("bbva-core-iframe.legacy-microfrontend")
            );
            SearchContext s2 = iframeHost.getShadowRoot();

            WebElement iframeContainer = s2.findElement(By.cssSelector("div#iframeContainer.iframe-container"));
            WebElement iframe = iframeContainer.findElement(By.cssSelector("iframe#bbvaIframe"));

            driver.switchTo().frame(iframe);

            try {
                WebElement containerBody = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("div#kyop-container-body")
                ));
                WebElement kyopBody = containerBody.findElement(By.cssSelector("div#kyop-body"));
                WebElement kyopBodyTable = kyopBody.findElement(By.cssSelector("table#kyop-boby-table"));
                WebElement td = kyopBodyTable.findElement(By.cssSelector("tbody tr td#kyop-body-table-td"));
                WebElement kyopIframeContainer = td.findElement(By.cssSelector("div#kyop-central-load-area-container"));
                WebElement iframeKyopCentral = kyopIframeContainer.findElement(By.cssSelector("iframe#kyop-central-load-area"));

                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframeKyopCentral));

                try {
                    WebElement contenedorSeccion = wait.until(
                            ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector("div.contenedor-seccion")
                            )
                    );

                    return mapearContenedorSeccion(contenedorSeccion);
                } finally {
                    driver.switchTo().defaultContent();
                }
            } catch (Exception innerException) {
                driver.switchTo().defaultContent();
                throw innerException;
            }
        } catch (TimeoutException | NoSuchElementException e) {
            log.error("Error al mapear contenido del iframe: {}", e.getMessage());
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {
                log.error("Ya se encuentra en el default context de driver manager");
            }

            result.put(Constantes.KEY_SUCCESS, false);
            result.put(Constantes.KEY_MESSAGE, e.getMessage());
            return result;
        }
    }

    public boolean clickMovimientos(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("bbva-btge-app-template#app__content")
            ));

            WebElement landing = body.findElement(By.cssSelector(
                    "bbva-btge-menurization-landing-solution-page#cells-template-bbva-btge-menurization-landing-solution"
            ));
            SearchContext s1 = landing.getShadowRoot();

            WebElement navMenu = s1.findElement(By.cssSelector(
                    "cells-template-paper-drawer-panel.state-is-visible[state='active']"
            ));
            WebElement appMain = navMenu.findElement(By.cssSelector("div[slot='app__main'].container"));

            WebElement iframeHost = appMain.findElement(
                    By.cssSelector("bbva-core-iframe.microfrontend-iframe")
            );

            SearchContext s2 = iframeHost.getShadowRoot();

            WebElement iframeContainer = s2.findElement(By.cssSelector("div#iframeContainer.iframe-container"));
            WebElement iframe = iframeContainer.findElement(By.cssSelector("iframe#bbvaIframe"));
            driver.switchTo().frame(iframe);
            try {
                WebElement homeHost = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("bbva-btge-menurization-landing-solution-home-page#cells-template-bbva-btge-menurization-landing-solution-home")
                ));
                SearchContext s4 = homeHost.getShadowRoot();

                WebElement mainContainer = s4.findElement(By.cssSelector("div[slot='app__main'].main-container"));
                WebElement wrapper = mainContainer.findElement(By.cssSelector("div.wrapper"));
                WebElement layout = wrapper.findElement(By.cssSelector("bbva-foundations-grid-default-layout.content-layout.container-cards"));
                WebElement leftCol = layout.findElement(By.cssSelector("div.column[slot='right']"));
                WebElement sectionCard = leftCol.findElement(By.cssSelector("div.section-card"));
                WebElement contentLevel = sectionCard.findElement(By.cssSelector("div.content-level"));

                WebElement link = contentLevel.findElement(
                        By.xpath(".//bbva-web-link[@class='link-level' and @role='button'][normalize-space(.)='Movimientos']")
                );

                wait.until(ExpectedConditions.visibilityOf(link));
                try {
                    wait.until(ExpectedConditions.elementToBeClickable(link)).click();
                } catch (ElementNotInteractableException e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
                }
            } finally {
                driver.switchTo().defaultContent();
            }
            return true;
        } catch (TimeoutException | NoSuchElementException e) {
            try { driver.switchTo().defaultContent(); } catch (Exception ignore) {}
            return false;
        }
    }

    public void busquedaMovimientos(WebDriver driver, String numeroCuenta, String fechaDesde, String fechaHasta) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("bbva-btge-app-template#app__content")
            ));
            WebElement landing = body.findElement(By.cssSelector("legacy-page#cells-template-legacy"));
            SearchContext s1 = landing.getShadowRoot();

            WebElement navMenu = s1.findElement(By.cssSelector("cells-template-paper-drawer-panel.state-is-visible[state='active']"));
            WebElement appMain = navMenu.findElement(By.cssSelector("div[slot='app__main'].container"));
            WebElement iframeHost = appMain.findElement(
                    By.cssSelector("bbva-core-iframe.legacy-microfrontend")
            );
            SearchContext s2 = iframeHost.getShadowRoot();

            WebElement iframeContainer = s2.findElement(By.cssSelector("div#iframeContainer.iframe-container"));
            WebElement iframe = iframeContainer.findElement(By.cssSelector("iframe#bbvaIframe"));

            driver.switchTo().frame(iframe);

            try {
                WebElement containerBody = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("div#kyop-container-body")
                ));
                WebElement kyopBody = containerBody.findElement(By.cssSelector("div#kyop-body"));
                WebElement kyopBodyTable = kyopBody.findElement(By.cssSelector("table#kyop-boby-table"));
                WebElement td = kyopBodyTable.findElement(By.cssSelector("tbody tr td#kyop-body-table-td"));
                WebElement kyopIframeContainer = td.findElement(By.cssSelector("div#kyop-central-load-area-container"));
                WebElement iframeKyopCentral = kyopIframeContainer.findElement(By.cssSelector("iframe#kyop-central-load-area"));

                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframeKyopCentral));
                try {
                    WebElement selectElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.name("AsuntoPropio")
                    ));

                    Select cuentaSelect = new Select(selectElement);

                    boolean cuentaEncontrada = false;
                    for (WebElement option : cuentaSelect.getOptions()) {
                        String optionValue = option.getAttribute("value");
                        if (optionValue.contains(numeroCuenta)) {
                            cuentaSelect.selectByValue(optionValue);
                            cuentaEncontrada = true;
                            break;
                        }
                    }

                    if (!cuentaEncontrada) {
                        log.error("No se encontró la cuenta con valor: {}", numeroCuenta);
                        throw new BbvaException("No se encontró cuenta");
                    }

                    WebElement radioVisual = wait.until(ExpectedConditions.elementToBeClickable(
                            By.cssSelector("input#radio1 + i.radio")
                    ));

                    if (!radioVisual.isSelected()) {
                        radioVisual.click();
                    }

                    MetodsGeneric.randomWait(500, 1000);

                    String[] partesDesde = fechaDesde.split("/");
                    String[] partesHasta = fechaHasta.split("/");

                    if (partesDesde.length != 3 || partesHasta.length != 3) {
                        log.error("Formato de fecha incorrecto. Use dd/mm/yyyy");
                    }

                    // Fecha Desde
                    WebElement diaDesde = driver.findElement(By.name("DiaDesde"));
                    WebElement mesDesde = driver.findElement(By.name("MesDesde"));
                    WebElement anioDesde = driver.findElement(By.name("AnioDesde"));

                    diaDesde.clear();
                    diaDesde.sendKeys(partesDesde[0]);
                    MetodsGeneric.randomWait(500, 800);

                    mesDesde.clear();
                    mesDesde.sendKeys(partesDesde[1]);
                    MetodsGeneric.randomWait(500, 800);

                    anioDesde.clear();
                    anioDesde.sendKeys(partesDesde[2]);
                    MetodsGeneric.randomWait(500, 800);

                    // Fecha Hasta
                    WebElement diaHasta = driver.findElement(By.name("DiaHasta"));
                    WebElement mesHasta = driver.findElement(By.name("MesHasta"));
                    WebElement anioHasta = driver.findElement(By.name("AnioHasta"));

                    diaHasta.clear();
                    diaHasta.sendKeys(partesHasta[0]);
                    MetodsGeneric.randomWait(500, 800);

                    mesHasta.clear();
                    mesHasta.sendKeys(partesHasta[1]);
                    MetodsGeneric.randomWait(500, 800);

                    anioHasta.clear();
                    anioHasta.sendKeys(partesHasta[2]);
                    MetodsGeneric.randomWait(500, 800);

                    WebElement btnConsultar = wait.until(ExpectedConditions.elementToBeClickable(
                            By.cssSelector("input[value='Consultar']")
                    ));

                    btnConsultar.click();
                } finally {
                    driver.switchTo().defaultContent();
                }
            } catch (Exception innerException) {
                driver.switchTo().defaultContent();
                throw innerException;
            }
        } catch (TimeoutException | NoSuchElementException e) {
            log.error("Error al mapear contenido del iframe: {}", e.getMessage());
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {
                log.error("Ya se encuentra en el default context de driver manager");
            }
        } catch (BbvaException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BbvaException("Interrupción durante la navegación",
                    "BBVA_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.error("Error al realizar busqueda por movimientos: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> extraerMovimientos(WebDriver driver) {
        List<Map<String, Object>> movimientos = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("bbva-btge-app-template#app__content")
            ));
            WebElement landing = body.findElement(By.cssSelector("legacy-page#cells-template-legacy"));
            SearchContext s1 = landing.getShadowRoot();

            WebElement navMenu = s1.findElement(By.cssSelector("cells-template-paper-drawer-panel.state-is-visible[state='active']"));
            WebElement appMain = navMenu.findElement(By.cssSelector("div[slot='app__main'].container"));
            WebElement iframeHost = appMain.findElement(
                    By.cssSelector("bbva-core-iframe.legacy-microfrontend")
            );
            SearchContext s2 = iframeHost.getShadowRoot();

            WebElement iframeContainer = s2.findElement(By.cssSelector("div#iframeContainer.iframe-container"));
            WebElement iframe = iframeContainer.findElement(By.cssSelector("iframe#bbvaIframe"));

            driver.switchTo().frame(iframe);

            WebElement containerBody = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div#kyop-container-body")
            ));
            WebElement kyopBody = containerBody.findElement(By.cssSelector("div#kyop-body"));
            WebElement kyopBodyTable = kyopBody.findElement(By.cssSelector("table#kyop-boby-table"));
            WebElement td = kyopBodyTable.findElement(By.cssSelector("tbody tr td#kyop-body-table-td"));
            WebElement kyopIframeContainer = td.findElement(By.cssSelector("div#kyop-central-load-area-container"));
            WebElement iframeKyopCentral = kyopIframeContainer.findElement(By.cssSelector("iframe#kyop-central-load-area"));

            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframeKyopCentral));
            movimientos = extractMovimientos(driver);
        } catch (TimeoutException | NoSuchElementException e) {
            log.error("Error al mapear contenido del iframe: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error al realizar busqueda por movimientos: {}", e.getMessage());
        }

        return movimientos;
    }

    public List<Map<String, Object>> extraerDetalleMovimientos(WebDriver driver, List<Map<String, Object>> listMovements) {
        List<Map<String, Object>> movimientosConDetalle = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement tabla = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.tb_data")));
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
            }
        } catch (TimeoutException | NoSuchElementException e) {
            log.error("Error al mapear contenido del iframe: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error al realizar busqueda por movimientos: {}", e.getMessage());
        }

        return movimientosConDetalle;
    }

    public boolean clickMovimientosHistoricos(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("bbva-btge-app-template#app__content")
            ));

            WebElement landing = body.findElement(By.cssSelector(
                    "bbva-btge-menurization-landing-solution-page#cells-template-bbva-btge-menurization-landing-solution"
            ));
            SearchContext s1 = landing.getShadowRoot();

            WebElement navMenu = s1.findElement(By.cssSelector(
                    "cells-template-paper-drawer-panel.state-is-visible[state='active']"
            ));
            WebElement appMain = navMenu.findElement(By.cssSelector("div[slot='app__main'].container"));

            WebElement iframeHost = appMain.findElement(
                    By.cssSelector("bbva-core-iframe.microfrontend-iframe")
            );

            SearchContext s2 = iframeHost.getShadowRoot();

            WebElement iframeContainer = s2.findElement(By.cssSelector("div#iframeContainer.iframe-container"));
            WebElement iframe = iframeContainer.findElement(By.cssSelector("iframe#bbvaIframe"));
            driver.switchTo().frame(iframe);
            try {
                WebElement homeHost = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("bbva-btge-menurization-landing-solution-home-page#cells-template-bbva-btge-menurization-landing-solution-home")
                ));
                SearchContext s4 = homeHost.getShadowRoot();

                WebElement mainContainer = s4.findElement(By.cssSelector("div[slot='app__main'].main-container"));
                WebElement wrapper = mainContainer.findElement(By.cssSelector("div.wrapper"));
                WebElement layout = wrapper.findElement(By.cssSelector("bbva-foundations-grid-default-layout.content-layout.container-cards"));
                WebElement leftCol = layout.findElement(By.cssSelector("div.column[slot='right']"));
                WebElement sectionCard = leftCol.findElement(By.cssSelector("div.section-card"));
                WebElement contentLevel = sectionCard.findElement(By.cssSelector("div.content-level"));

                WebElement link = contentLevel.findElement(
                        By.xpath(".//bbva-web-link[@class='link-level' and @role='button'][normalize-space(.)='Histórico de movimientos']")
                );

                wait.until(ExpectedConditions.visibilityOf(link));
                try {
                    wait.until(ExpectedConditions.elementToBeClickable(link)).click();
                } catch (ElementNotInteractableException e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
                }
            } finally {
                driver.switchTo().defaultContent();
            }
            return true;
        } catch (TimeoutException | NoSuchElementException e) {
            try { driver.switchTo().defaultContent(); } catch (Exception ignore) {}
            return false;
        }
    }

    public void busquedaMovimientosHistoricos(WebDriver driver, String numeroCuenta, String fechaDesde, String fechaHasta) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("bbva-btge-app-template#app__content")
            ));
            WebElement landing = body.findElement(By.cssSelector("legacy-page#cells-template-legacy"));
            SearchContext s1 = landing.getShadowRoot();

            WebElement navMenu = s1.findElement(By.cssSelector("cells-template-paper-drawer-panel.state-is-visible[state='active']"));
            WebElement appMain = navMenu.findElement(By.cssSelector("div[slot='app__main'].container"));
            WebElement iframeHost = appMain.findElement(
                    By.cssSelector("bbva-core-iframe.legacy-microfrontend")
            );
            SearchContext s2 = iframeHost.getShadowRoot();

            WebElement iframeContainer = s2.findElement(By.cssSelector("div#iframeContainer.iframe-container"));
            WebElement iframe = iframeContainer.findElement(By.cssSelector("iframe#bbvaIframe"));

            driver.switchTo().frame(iframe);

            try {
                WebElement containerBody = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("div#kyop-container-body")
                ));
                WebElement kyopBody = containerBody.findElement(By.cssSelector("div#kyop-body"));
                WebElement kyopBodyTable = kyopBody.findElement(By.cssSelector("table#kyop-boby-table"));
                WebElement td = kyopBodyTable.findElement(By.cssSelector("tbody tr td#kyop-body-table-td"));

                WebElement iframeKyopCentral = td.findElement(By.cssSelector("iframe#kyop-central-load-area"));

                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframeKyopCentral));
                try {
                    WebElement cBancElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.name("cBanc")
                    ));
                    WebElement cOficElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.name("cOfic")
                    ));
                    WebElement cContElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.name("cCont")
                    ));
                    WebElement cFoliElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.name("cFoli")
                    ));

                    WebElement selectElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.name("listaCuentas")
                    ));

                    Select cuentaSelect = new Select(selectElement);

                    boolean cuentaEncontrada = false;
                    String valorEncontrado = null;
                    for (WebElement option : cuentaSelect.getOptions()) {
                        String value = String.valueOf(option.getDomAttribute("value"));
                        if (value == null) continue;

                        String posterior = value.contains("@") ? value.substring(value.indexOf('@') + 1) : value;
                        String digits = posterior.replaceAll("\\D", "");
                        String normalized = removeIntercalatedCodeAfter8(digits);

                        if (numeroCuenta.equals(normalized)) {
                            cuentaEncontrada = true;
                            valorEncontrado = value;
                            log.info("✅ Cuenta encontrada: {}", value);
                            break;
                        }
                    }

                    if (cuentaEncontrada && valorEncontrado != null) {
                        // Extraer y rellenar las cajas de texto directamente
                        rellenarCajasTexto(valorEncontrado, cBancElement, cOficElement, cContElement, cFoliElement);
                    } else {
                        log.error("No se encontró la cuenta con valor: {}", numeroCuenta);
                    }

                    WebElement radioVisual = wait.until(ExpectedConditions.elementToBeClickable(
                            By.cssSelector("input#rad2 + i.radio")
                    ));

                    if (!radioVisual.isSelected()) {
                        radioVisual.click();
                    }

                    MetodsGeneric.randomWait(500, 1000);

                    String[] partesDesde = fechaDesde.split("/");
                    String[] partesHasta = fechaHasta.split("/");

                    if (partesDesde.length != 3 || partesHasta.length != 3) {
                        log.error("Formato de fecha incorrecto. Use dd/mm/yyyy");
                    }

                    // Fecha Desde
                    WebElement desde = driver.findElement(By.name("fechaDesde"));

                    desde.clear();
                    desde.sendKeys(partesDesde[0] + "-" + partesDesde[1] + "-" + partesDesde[2]);
                    MetodsGeneric.randomWait(500, 800);

                    // Fecha Hasta
                    WebElement hasta = driver.findElement(By.name("fechaHasta"));

                    hasta.clear();
                    hasta.sendKeys(partesHasta[0] + "-" + partesHasta[1] + "-" + partesHasta[2]);
                    MetodsGeneric.randomWait(500, 800);

                    //Combo segunda ordenacion
                    WebElement selectSegundaOrdenacion = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.name("indiceOrdenacion2")
                    ));

                    Select segundaOrdenacion = new Select(selectSegundaOrdenacion);

                    boolean segunda = false;
                    for (WebElement option : segundaOrdenacion.getOptions()) {
                        String optionValue = option.getAttribute("value");
                        if (optionValue.contains("4")) {
                            segundaOrdenacion.selectByValue(optionValue);
                            segunda = true;
                            break;
                        }
                    }

                    if (!segunda) {
                        log.error("No se encontró segunda ordenación con valor: {}", numeroCuenta);
                    }

                    WebElement btnAceptar = wait.until(ExpectedConditions.elementToBeClickable(
                            By.cssSelector("input[value='Aceptar']")
                    ));

                    btnAceptar.click();
                } finally {
                    driver.switchTo().defaultContent();
                }
            } catch (Exception innerException) {
                driver.switchTo().defaultContent();
                throw innerException;
            }
        } catch (TimeoutException | NoSuchElementException e) {
            log.error("Error al mapear contenido del iframe: {}", e.getMessage());
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignore) {
                log.error("Ya se encuentra en el default context de driver manager");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BbvaException("Interrupción durante la navegación",
                    "BBVA_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.error("Error al realizar busqueda por movimientos: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> extraerMovimientosHistoricos(WebDriver driver) {
        List<Map<String, Object>> movimientos = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("bbva-btge-app-template#app__content")
            ));
            WebElement landing = body.findElement(By.cssSelector("legacy-page#cells-template-legacy"));
            SearchContext s1 = landing.getShadowRoot();

            WebElement navMenu = s1.findElement(By.cssSelector("cells-template-paper-drawer-panel.state-is-visible[state='active']"));
            WebElement appMain = navMenu.findElement(By.cssSelector("div[slot='app__main'].container"));
            WebElement iframeHost = appMain.findElement(
                    By.cssSelector("bbva-core-iframe.legacy-microfrontend")
            );
            SearchContext s2 = iframeHost.getShadowRoot();

            WebElement iframeContainer = s2.findElement(By.cssSelector("div#iframeContainer.iframe-container"));
            WebElement iframe = iframeContainer.findElement(By.cssSelector("iframe#bbvaIframe"));

            driver.switchTo().frame(iframe);

            WebElement containerBody = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div#kyop-container-body")
            ));
            WebElement kyopBody = containerBody.findElement(By.cssSelector("div#kyop-body"));
            WebElement kyopBodyTable = kyopBody.findElement(By.cssSelector("table#kyop-boby-table"));
            WebElement td = kyopBodyTable.findElement(By.cssSelector("tbody tr td#kyop-body-table-td"));

            WebElement kyopIframeContainer = td.findElement(By.cssSelector("div#kyop-central-load-area-container"));
            WebElement iframeKyopCentral = kyopIframeContainer.findElement(By.cssSelector("iframe#kyop-central-load-area"));

            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframeKyopCentral));
            movimientos = extractMovimientosHistoricos(driver);
        } catch (TimeoutException | NoSuchElementException e) {
            log.error("Error al mapear contenido del iframe: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error al realizar busqueda por movimientos: {}", e.getMessage());
        }

        return movimientos;
    }

    public boolean clickSalir(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement appHost = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("bbva-btge-app-template#app__content")
            ));

            WebElement navMenu = appHost.findElement(By.cssSelector("bbva-btge-sidebar-menu.sidebar[slot='nav']"));

            SearchContext s1 = navMenu.getShadowRoot();

            WebElement container;
            try {
                container = s1.findElement(By.cssSelector("div.container"));
            } catch (NoSuchElementException e) {
                container = navMenu.findElement(By.cssSelector("div.container"));
            }

            WebElement sidebar = container.findElement(By.cssSelector("bbva-web-navigation-menu[role='navigation'][aria-label='Main menu']"));

            SearchContext s2 = sidebar.getShadowRoot();

            WebElement buttonSalir = s2.findElement(By.cssSelector("bbva-web-navigation-menu-item-action[role='button']"));

            wait.until(ExpectedConditions.visibilityOf(buttonSalir));
            try {
                wait.until(ExpectedConditions.elementToBeClickable(buttonSalir)).click();
                return true;
            } catch (ElementNotInteractableException e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", buttonSalir);
                return false;
            }

        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Extrae mocimientos de la tabla de resultado
     *
     * @param driver Manejador de página
     * @return {@link List<Map>}
     */
    /*private List<Map<String, Object>> extractMovimientos(WebDriver driver) {
        List<Map<String, Object>> movimientos = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement tabla = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("table.tb_data")
            ));

            List<WebElement> filas = tabla.findElements(By.cssSelector("tr.odd, tr.even"));

            for (WebElement fila : filas) {
                List<WebElement> columnas = fila.findElements(By.tagName("td"));

                if (columnas.size() >= 5) {
                    Map<String, Object> movimiento = extraerMovimientoDeFila(columnas);
                    movimientos.add(movimiento);

                    String itfText = getColumnText(columnas.get(3)).trim();
                    double itfValue = MetodsGeneric.parseSaldo(itfText);

                    if (Math.abs(itfValue) > 0.001) {
                        Map<String, Object> movimientoITF = crearMovimientoITF(movimiento, itfValue);
                        movimientos.add(movimientoITF);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error al extraer movimientos: {}", e.getMessage());
        }

        return movimientos;
    }*/

    /**
     * Extrae los datos de una fila de movimiento
     */
    /*private Map<String, Object> extraerMovimientoDeFila(List<WebElement> columnas) {
        Map<String, Object> movimiento = new HashMap<>();

        movimiento.put(Constantes.KEY_FECHA, getColumnText(columnas.get(0)));

        movimiento.put(Constantes.KEY_FECHA_VALOR, "");

        String descripcion = getColumnText(columnas.get(1));
        descripcion = descripcion.replaceAll("\\s+", " ").trim();
        movimiento.put(Constantes.KEY_DESCRIPCION, descripcion);

        movimiento.put(Constantes.KEY_OPERACION, MetodsGeneric.completarADiezDigitos(getColumnText(columnas.get(4))));

        String montoText = getColumnText(columnas.get(2));
        double monto = MetodsGeneric.parseSaldo(montoText);
        movimiento.put(Constantes.KEY_MONTO, monto);

        if (montoText.contains("-")) {
            movimiento.put(Constantes.KEY_TIPO, Constantes.VALOR_DEBITO);
        } else {
            movimiento.put(Constantes.KEY_TIPO, Constantes.VALOR_CREDITO);
        }

        movimiento.put(Constantes.KEY_SALDO, "");

        movimiento.put(Constantes.KEY_REFERENCIA, "");

        return movimiento;
    }*/

    /**
     * Crea un movimiento adicional para el ITF
     */
    /*private Map<String, Object> crearMovimientoITF(Map<String, Object> movimientoOriginal, double itfValue) {
        Map<String, Object> movimientoITF = new HashMap<>();

        movimientoITF.put("fecha", movimientoOriginal.get("fecha"));
        movimientoITF.put(Constantes.KEY_REFERENCIA, "");

        String descripcionOriginal = (String) movimientoOriginal.get(Constantes.KEY_DESCRIPCION);
        movimientoITF.put(Constantes.KEY_DESCRIPCION, descripcionOriginal + " - ITF");

        movimientoITF.put("operacion", "");

        movimientoITF.put("monto", Math.abs(itfValue));

        movimientoITF.put("tipo", "DEBITO");

        movimientoITF.put("saldo", "");

        return movimientoITF;
    }*/

    /**
     * Obtiene el texto de una columna, manejando elementos anidados
     */
    /*private String getColumnText(WebElement columna) {
        try {
            List<WebElement> enlaces = columna.findElements(By.tagName("a"));
            if (!enlaces.isEmpty()) {
                return enlaces.get(0).getText().trim();
            }

            return columna.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }*/

    /*private Map<String, Object> mapearContenedorSeccion(WebElement contenedorSeccion) {
        Map<String, Object> mapa = new HashMap<>();
        try {
            WebElement contenidoAcordion = contenedorSeccion.findElement(
                    By.cssSelector("div.contenido-acordion#cont_acordion0")
            );
            WebElement subContenedor = contenidoAcordion.findElement(
                    By.cssSelector("div.sub-contenedor-seccion#contenedor0_1")
            );

            WebElement contenidoAcordionInterno = subContenedor.findElement(
                    By.cssSelector("div.contenido-acordion#cont_sub-acordion0_1")
            );

            WebElement tabla = contenidoAcordionInterno.findElement(
                    By.cssSelector("table.tb_data.mobile_h#tabla-contenedor0_1")
            );
            mapa = mapearTabla(tabla);
        } catch (NoSuchElementException e) {
            log.warn("Error al mapear contenedor: {}", e.getMessage());
            mapa.put(Constantes.KEY_ERROR_CODE, e.getMessage());
        }

        return mapa;
    }*/

    /*private Map<String, Object> mapearTabla(WebElement tabla) {
        Map<String, Object> mapaTabla = new HashMap<>();

        try {
            List<Map<String, Object>> filas = new ArrayList<>();
            List<WebElement> filasElementos = tabla.findElements(
                    By.xpath(".//tbody/tr[not(contains(@class,'tb_column_header'))]")
            );

            for (WebElement fila : filasElementos) {
                Map<String, Object> filaMap = mapearFila(fila);
                filas.add(filaMap);
            }

            mapaTabla.put("data", filas);
            mapaTabla.put("count", filas.size());

        } catch (NoSuchElementException e) {
            log.warn("Error al mapear tabla: {}", e.getMessage());
            mapaTabla.put("error", e.getMessage());
        }

        return mapaTabla;
    }

    private Map<String, Object> mapearFila(WebElement fila) {
        Map<String, Object> filaMap = new HashMap<>();

        try {
            List<WebElement> celdas = fila.findElements(By.tagName("td"));

            if (celdas.size() >= 5) {
                WebElement enlace = celdas.get(0).findElement(By.cssSelector("a.enlace"));
                filaMap.put(Constantes.KEY_NUMERO_CUENTA, MetodsGeneric.cleanAccountNumber(enlace.getText().trim()));

                filaMap.put(Constantes.KEY_TIPO_CUENTA, celdas.get(1).getText().trim());

                String saldoContableStr = celdas.get(2).getText().trim();
                filaMap.put(Constantes.KEY_SALDO_CONT, MetodsGeneric.parseSaldo(saldoContableStr));

                String saldoDisponibleStr = celdas.get(3).getText().trim();
                filaMap.put(Constantes.KEY_SALDO_DISP, MetodsGeneric.parseSaldo(saldoDisponibleStr));

                filaMap.put(Constantes.KEY_MONEDA, celdas.get(4).getText().trim());
            }
        } catch (NoSuchElementException e) {
            log.warn("Error al mapear fila: {}", e.getMessage());
            filaMap.put("error", e.getMessage());
        }

        return filaMap;
    }

    private static class RowInfo {
        String operacion;
        String hrefAbsoluto;
        WebElement linkElem;
    }

    private Map<String, RowInfo> mapearFilasListado(WebDriver driver, WebElement tabla) {
        Map<String, RowInfo> mapa = new HashMap<>();
        List<WebElement> filas = tabla.findElements(By.cssSelector("tr.odd, tr.even"));
        for(WebElement fila : filas) {
            List<WebElement> tds = fila.findElements(By.tagName("td"));

            WebElement tdRef = tds.get(1);
            WebElement a;
            try {
                a = tdRef.findElement(By.cssSelector("a.enlace"));
            } catch (NoSuchElementException ex) {
                continue;
            }

            WebElement tdOperacion = tds.get(4);
            String operacion = normalizar(MetodsGeneric.completarADiezDigitos(tdOperacion.getText()));
            if (operacion.isEmpty()) continue;

            String hrefRel = a.getDomAttribute("href");
            String hrefAbs = hrefRel;
            if (hrefRel != null && !hrefRel.toLowerCase().startsWith("http")) {
                String base = driver.getCurrentUrl();
                hrefAbs = toAbsoluteUrl(base, hrefRel);
            }

            RowInfo ri = new RowInfo();
            ri.operacion = operacion;
            ri.hrefAbsoluto = hrefAbs;
            ri.linkElem = a;
            mapa.put(operacion, ri);
        }
        return mapa;
    }

    private String toAbsoluteUrl(String base, String rel) {
        try {
            java.net.URL baseUrl = new java.net.URL(base);
            java.net.URL abs = new java.net.URL(baseUrl, rel);
            return abs.toString();
        } catch (Exception e) {
            return rel;
        }
    }

    private String normalizar(String s) {
        if (s == null) return "";
        return s.replace('\u00A0', ' ').trim();
    }

    private Map<String, Object> abrirYExtraerDetalleConVolver(WebDriver driver, RowInfo info, WebDriverWait wait) throws InterruptedException {
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
    }

    private String extraerOperacionDesdeTitulo(String titulo) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(titulo);
        return m.find() ? m.group(1) : null;
    }

    private String removeIntercalatedCodeAfter8(String s) {
        if (s == null) return "";
        if (s.length() <= 10) return s; // demasiado corto para tener intercalado
        int split = 8;
        if (s.length() > split + 2) {
            return s.substring(0, split) + s.substring(split + 2);
        }
        return s;
    }*/

    /**
     * Intenta emparejar eliminando cualquier par de 2 dígitos después de la octava posición.
     * Útil por si el “código” no siempre está exactamente en índice 8 o puede variar.
     */
    /*private boolean maybeMatchByRemovingTwoDigits(String sourceDigits, String target) {
        if (sourceDigits == null) return false;
        int start = Math.min(8, sourceDigits.length()); // a partir de la 9na posición lógica
        for (int i = start; i + 2 <= sourceDigits.length(); i++) {
            String candidate = sourceDigits.substring(0, i) + sourceDigits.substring(i + 2);
            if (candidate.equals(target)) {
                return true;
            }
        }
        return false;
    }*/

    /**
     * Extrae movimientos historicos de la tabla de resultado
     *
     * @param driver Manejador de página
     * @return {@link List<Map>}
     */
    /*private List<Map<String, Object>> extractMovimientosHistoricos(WebDriver driver) {
        List<Map<String, Object>> movimientos = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement div = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div.oa")
            ));
            WebElement divTable = div.findElement(By.cssSelector("div#divtabla"));

            WebElement tabla = divTable.findElement(By.cssSelector("table.tb_data"));

            List<WebElement> filas = tabla.findElements(By.cssSelector("tr.odd, tr.even"));

            for (WebElement fila : filas) {
                List<WebElement> columnas = fila.findElements(By.tagName("td"));

                if (columnas.size() < 7) {
                    continue;
                }

                Map<String, Object> movimiento = extraerMovimientohistoricoDeFila(columnas);
                movimientos.add(movimiento);
            }

        } catch (Exception e) {
            log.error("Error al extraer movimientos: {}", e.getMessage());
        }

        return movimientos;
    }*/

    /**
     * Extrae los datos de una fila de movimiento
     */
    /*private Map<String, Object> extraerMovimientohistoricoDeFila(List<WebElement> columnas) {
        Map<String, Object> movimiento = new HashMap<>();

        // Col 0: F. Operación
        String fechaOperacion = DateUtils.parsearFechaBarraToSlash(getColumnSpanText(columnas.get(0)));
        movimiento.put(Constantes.KEY_FECHA, fechaOperacion);

        // Col 1: F. Valor
        String fechaValor = DateUtils.parsearFechaBarraToSlash(getColumnSpanText(columnas.get(1)));
        movimiento.put(Constantes.KEY_FECHA_VALOR, fechaValor);

        // Col 3: Nº. Doc.
        String numeroDoc = getColumnSpanText(columnas.get(3));
        movimiento.put(Constantes.KEY_OPERACION, numeroDoc);

        // Col 4: Concepto (texto visible en <a.enlace>)
        String descripcion = getConceptoTexto(columnas.get(4));
        descripcion = descripcion.replaceAll("\\s+", " ").trim();
        movimiento.put(Constantes.KEY_DESCRIPCION, descripcion);

        // Col 5: Importe (span con clase monedapos o txtselecroj)
        String montoText = getColumnSpanText(columnas.get(5));
        double monto = MetodsGeneric.parseSaldo(montoText); // asumes que maneja comas de miles y signo
        movimiento.put("monto", monto);

        movimiento.put(Constantes.KEY_REFERENCIA, "");

        if (monto < 0) {
            movimiento.put("tipo", "DEBITO");
        } else {
            movimiento.put("tipo", "CREDITO");
        }

        // Campos que antes dejabas vacíos para este layout
        movimiento.put("saldo", "");

        return movimiento;
    }

    // Obtiene el texto del primer <span> dentro de la celda, con normalización NBSP y trim
    private String getColumnSpanText(WebElement td) {
        try {
            WebElement span = td.findElement(By.tagName("span"));
            return normalize(span.getText());
        } catch (NoSuchElementException e) {
            return normalize(td.getText());
        }
    }

    // Obtiene el texto visible del concepto desde <td> (texto del <a class="enlace">)
    private String getConceptoTexto(WebElement td) {
        try {
            WebElement a = td.findElement(By.cssSelector("a.enlace"));
            return normalize(a.getText());
        } catch (NoSuchElementException e) {
            return normalize(td.getText());
        }
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replace('\u00A0', ' ').trim();
    }

    private void rellenarCajasTexto(String valorSeleccionado, WebElement cBanc, WebElement cOfic,
                                    WebElement cCont, WebElement cFoli) {
        try {
            String parteNumerica = valorSeleccionado.contains("@") ?
                    valorSeleccionado.substring(valorSeleccionado.indexOf('@') + 1) :
                    valorSeleccionado;

            String soloDigitos = parteNumerica.replaceAll("[^0-9]", "");

            log.info("Número completo extraído: {}", soloDigitos);

            if (soloDigitos.length() == 20) {
                String cBancValue = soloDigitos.substring(0, 4);        // primeros 4 dígitos
                String cOficValue = soloDigitos.substring(4, 8);        // siguientes 4 dígitos
                String cContValue = soloDigitos.substring(8, 10);       // siguientes 2 dígitos
                String cFoliValue = soloDigitos.substring(10, 20);     // últimos 10 dígitos

                log.info("Partes extraídas - cBanc: {}, cOfic: {}, cCont: {}, cFoli: {}",
                        cBancValue, cOficValue, cContValue, cFoliValue);

                rellenarCampo(cBanc, cBancValue, 4);
                rellenarCampo(cOfic, cOficValue, 4);
                rellenarCampo(cCont, cContValue, 2);
                rellenarCampo(cFoli, cFoliValue, 10);

                log.info("✅ Campos rellenados exitosamente");

            } else {
                log.error("El número extraído no tiene la longitud esperada. Longitud: {}", soloDigitos.length());
            }

        } catch (Exception e) {
            log.error("Error rellenando cajas de texto: {}", e.getMessage());
        }
    }

    private void rellenarCampo(WebElement campo, String valor, int maxLength) {
        try {
            campo.clear();
            MetodsGeneric.randomWait(200, 500);

            campo.sendKeys(valor);
            log.info("Campo {} rellenado con: {} (longitud: {})",
                    campo.getAttribute("name"), valor, valor.length());

            if (valor.length() > maxLength) {
                log.warn("El valor {} excede el maxlength {} del campo {}",
                        valor, maxLength, campo.getAttribute("name"));
            }

            MetodsGeneric.randomWait(200, 500);

        } catch (Exception e) {
            log.error("Error rellenando campo {}: {}", campo.getAttribute("name"), e.getMessage());
        }
    }*/
}