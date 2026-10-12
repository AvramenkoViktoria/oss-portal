@import java.util.List
@import java.util.Map
@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.protocol.ProtocolStatus
@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.ProtocolEntry
@param PageContext ctx
@param OrganizationDto org
@param Map<ProtocolStatus, List<ProtocolEntry>> columns
@param Map<Long, Long> unmet
!{String code = org.code().name().toLowerCase();}
@template.layout.page(ctx = ctx, title = "Протоколи " + org.code().name(), section = "cabinet-" + code, content = @`
    @template.components.cabinetHead(org = org, active = "protocols")
    <p class="muted">Протокол проходить три етапи: чернетка → затверджено → опубліковано. Чернетка створюється автоматично, щойно засідання позначено проведеним.</p>
    <div class="board">
        @for(var status : ProtocolStatus.values())
            <section class="board-col">
                <h3>
                    <span>${Labels.protocolStatus(status)}</span>
                    @template.components.chip(css = Labels.protocolCss(status), text = String.valueOf(columns.get(status).size()))
                </h3>
                @if(columns.get(status).isEmpty())
                    <p class="muted small">Порожньо</p>
                @endif
                @for(var e : columns.get(status))
                    <a class="board-card" href="/cabinet/${code}/protocols/${e.protocol().id()}">
                        <strong>${e.meeting().title()}</strong>
                        <div class="muted small">${ctx.fmt().date(e.meeting().scheduledAt())}</div>
                        @if(status == ProtocolStatus.DRAFT)
                            @if(unmet.getOrDefault(e.protocol().id(), 0L) > 0)
                                <div class="small" style="color:var(--draft)">Не виконано умов: ${unmet.get(e.protocol().id())}</div>
                            @else
                                <div class="small" style="color:var(--published)">Готово до затвердження</div>
                            @endif
                        @elseif(status == ProtocolStatus.APPROVED)
                            <div class="small" style="color:var(--approved)">Очікує публікації</div>
                        @endif
                    </a>
                @endfor
            </section>
        @endfor
    </div>
`)
