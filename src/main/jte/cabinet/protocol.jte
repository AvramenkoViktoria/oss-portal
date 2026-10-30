@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.protocol.ProtocolStatus
@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.ProtocolPage
@param PageContext ctx
@param OrganizationDto org
@param ProtocolPage page
@param boolean editable
!{String code = org.code().name().toLowerCase();}
!{var protocol = page.protocol();}
!{String base = "/cabinet/" + code + "/protocols/" + protocol.id();}
@template.layout.page(ctx = ctx, title = "Протокол: " + page.meeting().title(), section = "cabinet-" + code, content = @`
    <div class="breadcrumbs"><a href="/cabinet/${code}/meetings">Засідання ${org.code().name()}</a> / <a href="/cabinet/${code}/meetings/${page.meeting().id()}">${page.meeting().title()}</a> / Протокол</div>
    <div class="page-head">
        <div>
            <h1>Протокол засідання «${page.meeting().title()}»</h1>
            <div class="row">
                @template.components.chip(css = Labels.protocolCss(protocol.status()), text = Labels.protocolStatus(protocol.status()))
                <a class="small" href="/cabinet/${code}/meetings/${page.meeting().id()}">засідання</a>
                @if(protocol.status() == ProtocolStatus.PUBLISHED)
                    <a class="small" href="/protocols/${protocol.id()}">публічна сторінка</a>
                @endif
            </div>
        </div>
    </div>

    <section class="card">
        @template.components.lifecycle(ctx = ctx, page = page)
        @if(protocol.status() == ProtocolStatus.APPROVED)
            <div class="lock-banner">🔒 Протокол затверджено: документ і голосування заблоковано. Лишилося опублікувати.</div>
        @elseif(protocol.status() == ProtocolStatus.PUBLISHED)
            <div class="lock-banner published">✓ Протокол опубліковано й доступний усім. Зміни неможливі.</div>
        @endif
    </section>

    <div class="split section">
        <div>
            <section class="card">
                <div class="card-head"><h2 style="margin:0">Документ</h2></div>
                @if(protocol.hasFile())
                    <p>
                        <strong>${protocol.fileName()}</strong>
                        — <a href="/protocols/${protocol.id()}/file">завантажити</a>
                    </p>
                @else
                    <p class="muted">Документ ще не прикріплено.</p>
                @endif
                @if(editable)
                    <form method="post" action="${base}/file" enctype="multipart/form-data" class="row">
                        <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                        <input type="file" name="file" accept=".pdf,.docx" required>
                        <button class="btn secondary" type="submit">${protocol.hasFile() ? "Замінити" : "Прикріпити"}</button>
                    </form>
                    <p class="muted small" style="margin:.5rem 0 0">Приймаються PDF та DOCX, до 10 МБ. У протоколі обов’язково має бути розділ «Результати голосування», інакше документ не буде прийнято.</p>
                @endif
            </section>

            <section class="card">
                <div class="card-head"><h2 style="margin:0">Голосування</h2></div>
                @if(page.votings().isEmpty())
                    <p class="muted">Голосувань поки немає.</p>
                @endif
                @for(var v : page.votings())
                    @template.components.votingBlock(v = v)
                @endfor
                <p class="muted small" style="margin:.75rem 0 0">Голосування формуються автоматично з розділів «Результати голосування» завантаженого протоколу. Їх не можна створити чи змінити вручну: щоб виправити, завантажте оновлений документ.</p>
            </section>
        </div>

        <aside>
            @if(protocol.status() == ProtocolStatus.DRAFT)
                <section class="card">
                    <h3>Наступний крок: затвердження</h3>
                    <ul class="checklist">
                        @for(var r : page.requirements())
                            <li>
                                <span class="check ${r.met() ? "ok" : "todo"}">${r.met() ? "✓" : ""}</span>
                                <span class="${r.met() ? "" : "muted"}">${r.description()}</span>
                            </li>
                        @endfor
                    </ul>
                    <form method="post" action="${base}/approve">
                        <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                        <button class="btn" type="submit" disabled="${!page.requirementsMet()}">Затвердити протокол</button>
                    </form>
                    @if(!page.requirementsMet())
                        <p class="muted small" style="margin:.5rem 0 0">Кнопка стане активною, коли всі умови виконано.</p>
                    @endif
                </section>
            @elseif(protocol.status() == ProtocolStatus.APPROVED)
                <section class="card">
                    <h3>Наступний крок: публікація</h3>
                    <p>Після публікації протокол стане видимим усім, а в канал ${Labels.messenger(org.messenger())} піде анонс із посиланням.</p>
                    <div class="notice-preview">Канал: ${org.channelId()}</div>
                    <form method="post" action="${base}/publish" style="margin-top:1rem" onsubmit="return confirm('Опублікувати протокол? Дію неможливо скасувати.')">
                        <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
                        <button class="btn success" type="submit">Опублікувати</button>
                    </form>
                </section>
            @else
                <section class="card">
                    <h3>Протокол опубліковано</h3>
                    <p>Анонс із посиланням надіслано в ${Labels.messenger(org.messenger())}. Дивіться стан доставки в розділі адміністрування.</p>
                    <a class="btn secondary" href="/protocols/${protocol.id()}">Відкрити публічну сторінку</a>
                </section>
            @endif

            <section class="card">
                <h3>Історія</h3>
                <ul class="timeline">
                    <li>Засідання відбулося<div class="muted small">${ctx.fmt().dateTime(page.meeting().scheduledAt())}</div></li>
                    <li>Створено чернетку<div class="muted small">${ctx.fmt().dateTime(protocol.createdAt())}</div></li>
                    @if(protocol.approvedAt() != null)
                        <li>Затверджено@if(page.approvedByName() != null): ${page.approvedByName()}@endif<div class="muted small">${ctx.fmt().dateTime(protocol.approvedAt())}</div></li>
                    @else
                        <li class="future">Затвердження</li>
                    @endif
                    @if(protocol.publishedAt() != null)
                        <li>Опубліковано@if(page.publishedByName() != null): ${page.publishedByName()}@endif<div class="muted small">${ctx.fmt().dateTime(protocol.publishedAt())}</div></li>
                    @else
                        <li class="future">Публікація та анонс</li>
                    @endif
                </ul>
            </section>
        </aside>
    </div>
`)
