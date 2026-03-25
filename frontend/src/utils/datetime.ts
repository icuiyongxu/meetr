import dayjs from 'dayjs'

export function toLocalDateTimeString(input: string | Date) {
  return dayjs(input).format('YYYY-MM-DDTHH:mm:ss')
}

export function formatRange(start: string, end: string) {
  return `${dayjs(start).format('YYYY-MM-DD HH:mm')} - ${dayjs(end).format('HH:mm')}`
}

export function isPast(input: string | Date) {
  return dayjs(input).isBefore(dayjs())
}

